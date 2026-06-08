package com.example.antiphishingapp.feature.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antiphishingapp.feature.model.AnalysisResponse
import com.example.antiphishingapp.feature.repository.AnalysisRepository
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class AnalysisViewModel : ViewModel() {

    private val repository = AnalysisRepository()

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    private val _result = MutableLiveData<AnalysisResponse?>()
    val result: LiveData<AnalysisResponse?> get() = _result

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    // ── 일반 분석 요청 ────────────────────────────────────────────
    fun analyzeDocument(file: MultipartBody.Part) {
        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            repository.analyzeDocument(
                file = file,
                onResult = { res ->
                    _loading.postValue(false)
                    _result.postValue(res)
                },
                onError = { err ->
                    _loading.postValue(false)
                    _error.postValue(err.message ?: "분석 실패")
                }
            )
        }
    }

    // ── 강제 분석 요청 (문서 판별 건너뜀) ────────────────────────
    fun analyzeDocumentForce(file: MultipartBody.Part) {
        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            repository.analyzeDocumentForce(
                file = file,
                onResult = { res ->
                    _loading.postValue(false)
                    _result.postValue(res)
                },
                onError = { err ->
                    _loading.postValue(false)
                    _error.postValue(err.message ?: "분석 실패")
                }
            )
        }
    }

    fun resetResult() {
        _result.value = null
        _error.value = null
    }
}