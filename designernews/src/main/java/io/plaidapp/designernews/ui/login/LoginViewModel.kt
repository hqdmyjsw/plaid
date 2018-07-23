/*
 * Copyright 2018 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.plaidapp.designernews.ui.login

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.plaidapp.R
import io.plaidapp.core.data.CoroutinesContextProvider
import io.plaidapp.core.data.Result
import io.plaidapp.core.designernews.data.login.LoginRepository
import io.plaidapp.core.util.event.Event
import io.plaidapp.core.util.exhaustive
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch

/**
 * View Model for [LoginActivity]
 * TODO move the rest of the logic from activity here.
 */
class LoginViewModel(
    private val loginRepository: LoginRepository,
    private val contextProvider: CoroutinesContextProvider
) : ViewModel() {

    private var currentJob: Job? = null

    private val _uiState = MutableLiveData<LoginUiModel>()
    val uiState: LiveData<LoginUiModel>
        get() = _uiState

    init {
        // at view model initiation, the login is not valid so the login button should be disabled
        enableLogin(false)
    }

    fun login(username: String, password: String) {
        // only allow one login at a time
        if (currentJob?.isActive == true) {
            return
        }
        currentJob = launchLogin(username, password)
    }

    private fun launchLogin(username: String, password: String) = launch(contextProvider.io) {
        if (!isLoginValid(username, password)) {
            return@launch
        }
        showLoading()
        val result = loginRepository.login(username, password)

        when (result) {
            is Result.Success -> {
                val user = result.data
                emitUiState(
                    false,
                    null,
                    Event(SuccessLoginUiModel(user.displayName.toLowerCase(), user.portraitUrl)),
                    false
                )
            }
            is Result.Error -> {
                emitUiState(
                    false,
                    Event(R.string.login_failed),
                    null,
                    true
                )
            }
            is Result.Loading -> {
                /* we ignore the loading state */
            }
        }.exhaustive
    }

    private fun showLoading() {
        emitUiState(
            true,
            null,
            null,
            true
        )
    }

    override fun onCleared() {
        super.onCleared()
        // when the VM is destroyed, cancel the running job.
        currentJob?.cancel()
    }

    fun loginDataChanged(username: String, password: String) {
        enableLogin(isLoginValid(username, password))
    }

    private fun isLoginValid(username: String, password: String): Boolean {
        return username.isNotBlank() && password.isNotBlank()
    }

    private fun enableLogin(enabled: Boolean) {
        emitUiState(false, null, null, enabled)
    }

    private fun emitUiState(
        showProgress: Boolean,
        showError: Event<Int>?,
        showSuccess: Event<SuccessLoginUiModel>?,
        enableLoginButton: Boolean
    ) {
        val uiModel = LoginUiModel(showProgress, showError, showSuccess, enableLoginButton)
        _uiState.postValue(uiModel)
    }
}

/**
 * UI model for [LoginActivity]
 */
data class LoginUiModel(
    val showProgress: Boolean,
    val showError: Event<Int>?,
    val showSuccess: Event<SuccessLoginUiModel>?,
    val enableLoginButton: Boolean
)

/**
 * UI Model for login success
 */
data class SuccessLoginUiModel(
    val displayName: String,
    val portraitUrl: String?
)
