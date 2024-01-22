package com.plcoding.stockmarketapp.data.repository

import com.opencsv.CSVReader
import com.plcoding.stockmarketapp.data.csv.CSVParser
import com.plcoding.stockmarketapp.data.csv.CompanyListingParser
import com.plcoding.stockmarketapp.data.local.StockDatabase
import com.plcoding.stockmarketapp.data.mapper.toCompanyListing
import com.plcoding.stockmarketapp.data.mapper.toCompanyListingEntity
import com.plcoding.stockmarketapp.data.remote.StockApi
import com.plcoding.stockmarketapp.domain.model.CompanyListing
import com.plcoding.stockmarketapp.domain.repository.StockRepository
import com.plcoding.stockmarketapp.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class StockRepositoryImpl @Inject constructor(
    private val api: StockApi,
    private val db: StockDatabase,
    private val companyListingParser: CSVParser<CompanyListing>
): StockRepository {

    private val dao = db.dao

    override suspend fun getCompanyListings(
        fetchFromRemote: Boolean,
        query: String
    ): Flow<Resource<List<CompanyListing>>> {
        return flow {
            emit(Resource.Loading())
            val localListings = dao.searchCompanyListing(query)
            emit(Resource.Success(
                data = localListings.map { it.toCompanyListing() }
            ))
            //Kiểm tra dữ liệu trong DB có rỗng không, query truyền vào phải khác rỗng
            val isDbEmpty = localListings.isEmpty() && query.isBlank()

            //Kiểm tra dữ liệu đã có sẵn để load thêm khi vuốt không
            //Nếu dữ liệu có sẵn thì lấy dữ liệu từ cache để load
            val shouldJustFromCache = !isDbEmpty && !fetchFromRemote
            if (shouldJustFromCache) {
                emit(Resource.Loading(false))
                return@flow
            }

            val remoteListings = try {
                val response = api.getListings()
                companyListingParser.parse(response.byteStream())
            } catch (e: IOException) {
                e.printStackTrace()
                emit(Resource.Error("Couldn't not load data"))
                null
            } catch (e: HttpException) {
                e.printStackTrace()
                emit(Resource.Error("Couldn't not load data"))
                null
            }

            //Không nên lấy dữ liệu trực tiếp từ api truyền vào UI
            //Nên lấy dữ liệu từ API insert vào Database, sau đó lấy từ database truyền vào UI
            remoteListings?.let { listings ->
                dao.clearCompanyListing()
                dao.insertCompanyListing(listings.map { it.toCompanyListingEntity() })

                emit(Resource.Success(
                    data = dao
                        .searchCompanyListing("")
                        .map { it.toCompanyListing() }
                ))
                emit(Resource.Loading(false))

            }
        }
    }

}