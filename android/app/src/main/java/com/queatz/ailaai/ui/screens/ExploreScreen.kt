package com.queatz.ailaai.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import app.ailaai.api.cards
import app.ailaai.api.myGeo
import app.ailaai.api.savedCards
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.SwipeResult
import com.queatz.ailaai.extensions.distance
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.navigate
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberSavableStateOf
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.scrollToTop
import com.queatz.ailaai.extensions.sortedDistinct
import com.queatz.ailaai.extensions.swipe
import com.queatz.ailaai.extensions.toGeo
import com.queatz.ailaai.extensions.toLatLng
import com.queatz.ailaai.helpers.ResumeEffect
import com.queatz.ailaai.helpers.locationSelector
import com.queatz.ailaai.item.InventoryDialog
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.trade.TradeItemDialog
import com.queatz.ailaai.ui.components.AppHeader
import com.queatz.ailaai.ui.components.CardList
import com.queatz.ailaai.ui.components.CardsBar
import com.queatz.ailaai.ui.components.DisplayText
import com.queatz.ailaai.ui.components.LocationScaffold
import com.queatz.ailaai.ui.components.MainTab
import com.queatz.ailaai.ui.components.MainTabs
import com.queatz.ailaai.ui.components.PageInput
import com.queatz.ailaai.ui.components.ScanQrCodeButton
import com.queatz.ailaai.ui.components.SearchFieldAndAction
import com.queatz.ailaai.ui.components.swipeMainTabs
import com.queatz.ailaai.ui.state.latLngSaver
import com.queatz.db.Card
import com.queatz.db.Inventory
import com.queatz.db.InventoryItemExtended
import com.queatz.db.TakeInventoryItem
import inventoriesNear
import inventory
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.launch
import takeInventory

var exploreInitialCategory: String? = null

private var cache = mutableMapOf<MainTab, List<Card>>()

@Composable
fun ExploreScreen() {
    val paidString = stringResource(R.string.paid)
    val state = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var value by rememberSavableStateOf("")
    var shownValue by rememberSavableStateOf(value)
    var filterPaid by rememberSavableStateOf(false)
    var selectedCategory by rememberSaveable { mutableStateOf(exploreInitialCategory) }
    var categories by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var geo: LatLng? by rememberSaveable(stateSaver = latLngSaver()) { mutableStateOf(null) }
    var mapGeo: LatLng? by rememberSaveable(stateSaver = latLngSaver()) { mutableStateOf(null) }
    var isError by rememberStateOf(false)
    var showAsMap by rememberSavableStateOf(true)
    var offset by remember { mutableIntStateOf(0) }
    var hasMore by rememberStateOf(true)
    var shownGeo: LatLng? by remember { mutableStateOf(null) }
    val nav = nav
    val locationSelector = locationSelector(
        geo,
        { geo = it },
        nav.context as Activity
    )
    var tab by rememberSavableStateOf(MainTab.Local)
    var shownTab by rememberSavableStateOf(tab)
    var cards by remember { mutableStateOf(cache[tab] ?: emptyList()) }
    var isLoading by rememberStateOf(cards.isEmpty())
    val filters by remember(filterPaid, tab) {
        mutableStateOf(
            when (tab) {
                MainTab.Local, MainTab.Friends -> {
                    if (cards.any { it.pay != null }) {
                        listOf(
                            SearchFilter(
                                paidString,
                                Icons.Outlined.Payments,
                                filterPaid
                            ) {
                                filterPaid = !filterPaid
                            }
                        )
                    } else {
                        emptyList()
                    }
                }

                else -> emptyList()
            }
        )
    }
    var inventories by rememberStateOf(emptyList<Inventory>())
    var showInventory by rememberStateOf<String?>(null)
    var showInventoryDialog by rememberStateOf<List<InventoryItemExtended>?>(null)
    var showInventoryItemDialog by rememberStateOf<InventoryItemExtended?>(null)
    var showBar by rememberStateOf(true)
    val mapControl = remember { MapControl() }

    suspend fun reloadInventories() {
        if (showAsMap && (mapGeo ?: geo) != null) {
            api.inventoriesNear((mapGeo ?: geo)!!.toGeo()) {
                inventories = it
            }
        } else {
            inventories = emptyList()
        }
    }


    LaunchedEffect(geo) {
        geo?.let {
            api.myGeo(it.toGeo())
        }
    }

    LaunchedEffect(cards) {
        cache[tab] = cards
    }

    LaunchedEffect(showAsMap) {
        if (showAsMap) {
            tab = MainTab.Local
        }
    }

    LaunchedEffect(showAsMap, mapGeo, geo) {
        reloadInventories()
    }

    LaunchedEffect(showInventory) {
        showInventory?.let {
            api.inventory(it) {
                showInventoryDialog = it
            }
        }
    }

    showInventoryDialog?.let { items ->
        InventoryDialog(
            {
                showInventory = null
                showInventoryDialog = null
            },
            items = items
        ) {
            showInventoryItemDialog = it
        }
    }

    fun updateCategories() {
        selectedCategory = selectedCategory ?: exploreInitialCategory
        categories = ((exploreInitialCategory.inList() + cards
            .flatMap { it.categories ?: emptyList() }) + selectedCategory.inList())
            .sortedDistinct()
        exploreInitialCategory = null
    }

    fun onNewPage(page: List<Card>, clear: Boolean) {
        val oldSize = if (clear) 0 else cards.size
        cards = if (clear) {
            page
        } else {
            (cards + page).distinctBy { it.id }
        }

        offset = cards.size
        hasMore = cards.size > oldSize
        isError = false
        isLoading = false
        shownGeo = geo
        shownValue = value
        shownTab = tab

        updateCategories()

        if (clear) {
            scope.launch {
                state.scrollToTop()
            }
        }
    }

    fun clear() {
        offset = 0
        hasMore = true
        isLoading = true
        cards = emptyList()
    }

    suspend fun loadMore(
        reload: Boolean = false
    ) {
        val geo = (mapGeo?.takeIf { showAsMap } ?: geo) ?: return
        if (reload) {
            offset = 0
            hasMore = true
        }
        when (tab) {
            MainTab.Friends,
            MainTab.Local -> {
                api.cards(
                    geo.toGeo(),
                    offset = offset,
                    paid = filterPaid.takeIf { it },
                    search = value.notBlank,
                    public = tab == MainTab.Local,
                    onError = { ex ->
                        if (ex is CancellationException || ex is InterruptedException) {
                            // Ignore, probably geo or search value changed, keep isLoading = true
                        } else {
                            isLoading = false
                            isError = true
                        }
                    }
                ) {
                    onNewPage(it, reload)
                }
            }

            MainTab.Saved -> {
                api.savedCards(
                    offset = offset,
                    search = value.notBlank,
                    onError = { ex ->
                        if (ex is CancellationException || ex is InterruptedException) {
                            // Ignore, probably geo or search value changed, keep isLoading = true
                        } else {
                            isLoading = false
                            isError = true
                        }
                    }) {
                    onNewPage(it.mapNotNull { it.card }, reload)
                }
            }
        }
    }

    LaunchedEffect(filterPaid) {
        loadMore(reload = true)
    }

    LaunchedEffect(geo, mapGeo, value, tab) {
        if (geo == null && mapGeo == null) {
            return@LaunchedEffect
        }

        val moveUnder100 = shownGeo?.let { shownGeo ->
            (mapGeo?.takeIf { showAsMap } ?: geo)?.distance(shownGeo)?.let { it < 100 }
        } ?: true

        // Don't reload if moving < 100m
        if (shownGeo != null && moveUnder100 && shownValue == value && shownTab == tab) {
            return@LaunchedEffect
        }

        if (shownTab != tab) {
            clear()
        }

        // The map doesn't clear for geo updates, but should for value and tab changes
        loadMore(
            reload = !moveUnder100 || shownValue != value
        )
    }


    showInventoryItemDialog?.let { item ->
        val quantity = item.inventoryItem?.quantity ?: 0.0
        TradeItemDialog(
            {
                showInventoryItemDialog = null
            },
            item,
            initialQuantity = quantity,
            maxQuantity = quantity,
            isAdd = true,
            isMine = true,
            enabled = true,
            confirmButton = stringResource(R.string.pick_up),
            onQuantity = {
                scope.launch {
                    api.takeInventory(
                        item.inventoryItem!!.inventory!!,
                        listOf(TakeInventoryItem(item.inventoryItem!!.id!!, it))
                    ) {
                        showInventory?.let {
                            api.inventory(it) {
                                if (it.isEmpty()) {
                                    showInventory = null
                                    showInventoryDialog = null
                                } else {
                                    showInventoryDialog = it
                                }
                            }
                        }
                        showInventoryItemDialog = null
                        reloadInventories()
                    }
                }
            }
        )
    }


    ResumeEffect {
        loadMore()
    }

    LocationScaffold(
        geo,
        locationSelector,
        appHeader = {
            AppHeader(
                if (showAsMap) stringResource(R.string.map) else stringResource(R.string.cards),
                {},
            ) {
                ScanQrCodeButton()
            }
        },
        rationale = {
            // todo: translate
            DisplayText("Discover and post pages in your town.")
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppHeader(
                if (showAsMap) stringResource(R.string.map) else stringResource(R.string.cards),
                {
                    scope.launch {
                        state.scrollToTop()
                    }
                }
            ) {
                if (showAsMap) {
                IconButton(
                    onClick = {
                        showBar = !showBar
                    }
                ) {
                    Icon(if (showBar) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                }
                    }
                IconButton({
                    showAsMap = !showAsMap
                }) {
                    Icon(if (showAsMap) Icons.Outlined.ViewAgenda else Icons.Outlined.Map, stringResource(R.string.map))
                }
                ScanQrCodeButton()
            }

            val cardsOfCategory = remember(cards, selectedCategory) {
                if (selectedCategory == null) cards else cards.filter {
                    it.categories?.contains(
                        selectedCategory
                    ) == true
                }
            }

            var viewportHeight by remember { mutableIntStateOf(0) }

            if (showAsMap) {
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        if (cards.isNotEmpty()) {
                            AnimatedVisibility(showBar) {
                                CardsBar(
                                    cardsOfCategory,
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    onLongClick = {
                                        scope.launch {
                                            mapControl.recenter(it.geo?.toLatLng() ?: return@launch)
                                        }
                                    }
                                ) {
                                    nav.navigate(AppNav.Page(it.id!!))
                                }
                            }
                        }
                        MapScreen(
                            control = mapControl,
                            cards = cardsOfCategory,
                            inventories = inventories,
                            bottomPadding = viewportHeight,
                            onCard = {
                                nav.navigate(AppNav.Page(it))
                            },
                            onInventory = {
                                showInventory = it
                            }
                        ) {
                            mapGeo = it
                        }
                    }
                    PageInput(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .onPlaced { viewportHeight = it.boundsInParent().size.height.toInt() }
                    ) {
                        SearchContent(
                            locationSelector,
                            isLoading,
                            filters,
                            categories,
                            selectedCategory
                        ) {
                            selectedCategory = it
                        }
                        SearchFieldAndAction(
                            value,
                            { value = it },
                            placeholder = stringResource(R.string.search),
                            action = {
                                Icon(Icons.Outlined.Edit, stringResource(R.string.your_cards))
                            },
                            onAction = {
                                nav.navigate(AppNav.Me)
                            },
                        )
                    }
                }
            } else {
                val me = me
                MainTabs(tab, { tab = it })
                CardList(
                    state = state,
                    cards = cardsOfCategory,
                    isMine = { it.person == me?.id },
                    geo = geo,
                    onChanged = {
                        scope.launch {
                            clear()
                            loadMore(reload = true)
                        }
                    },
                    isLoading = isLoading,
                    isError = isError,
                    value = value,
                    valueChange = { value = it },
                    placeholder = stringResource(R.string.search),
                    hasMore = hasMore,
                    onLoadMore = {
                        loadMore()
                    },
                    action = {
                        Icon(Icons.Outlined.Edit, stringResource(R.string.your_cards))
                    },
                    onAction = {
                        nav.navigate(AppNav.Me)
                    },
                    modifier = Modifier
                        .swipeMainTabs {
                            when (val it = MainTab.entries.swipe(tab, it)) {
                                is SwipeResult.Previous -> {
                                    nav.navigate(AppNav.Schedule)
                                }

                                is SwipeResult.Next -> {
                                    nav.navigate(AppNav.Inventory)
                                }

                                is SwipeResult.Select<*> -> {
                                    tab = it.item as MainTab
                                }
                            }
                        }
                ) {
                    SearchContent(
                        locationSelector,
                        isLoading,
                        filters,
                        categories,
                        selectedCategory
                    ) {
                        selectedCategory = it
                    }
                }
            }
        }
    }
}

