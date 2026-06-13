package com.appetizers.spotra.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appetizers.spotra.domain.usecase.GetStartRouteUseCase
import com.appetizers.spotra.domain.usecase.StartRoute
import com.appetizers.spotra.presentation.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(
    private val getStartRoute: GetStartRouteUseCase
) : ViewModel() {
    private val _startRoute = MutableStateFlow<String>(Routes.Loading)
    val startRoute: StateFlow<String> = _startRoute.asStateFlow()

    init {
        resolveStartRoute()
    }

    fun resolveStartRoute() {
        viewModelScope.launch {
            _startRoute.value = when (getStartRoute()) {
                StartRoute.Home -> Routes.Home
                StartRoute.Name -> Routes.Name
                StartRoute.Welcome -> Routes.Welcome
            }
        }
    }

    class Factory(
        private val getStartRoute: GetStartRouteUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AppViewModel(getStartRoute) as T
    }
}
