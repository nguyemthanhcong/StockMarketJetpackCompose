package com.plcoding.stockmarketapp.presentation.company_listings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.stockmarketapp.domain.repository.StockRepository
import com.plcoding.stockmarketapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompanyListingsViewModel @Inject constructor(
    private val repository: StockRepository
):ViewModel() {

    var state by mutableStateOf(CompanyListingState())
    private var searchJob: Job? = null

    init {
        getCompanyListing()
    }

    fun onEvent(event: CompanyListingEvent) {
        when(event) {
            is CompanyListingEvent.Refresh -> {
                getCompanyListing(fetchFromRemote = false)
            }

            is CompanyListingEvent.OnSearchQueryChange -> {
                state = state.copy(searchQuery = event.query)
                if (searchJob?.isCompleted == true) {
                    searchJob?.cancel()
                }
//                searchJob?.cancel() //Nếu đang có một job tìm kiếm thì hủy các tìm kiếm khác
                searchJob = viewModelScope.launch {
                    delay(500L)
                    getCompanyListing()
                }
            }
        }
    }

    private fun getCompanyListing(
        query: String = state.searchQuery.lowercase(),
        fetchFromRemote: Boolean = false
    ) {
        viewModelScope.launch {
            repository.getCompanyListings(fetchFromRemote, query)
                .collect{result ->
                    when(result) {
                        is Resource.Success ->  {
                            result.data?.let {listings ->
                                state = state.copy(
                                    companies = listings
                                )
                            }
                        }
                        is Resource.Error -> Unit

                        is Resource.Loading -> {
                            state = state.copy(isLoading = result.isLoading)
                        }
                    }
                }
        }
    }
}