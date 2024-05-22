package top.nabil.nugazlah.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.nabil.nugazlah.data.remote.request.RequestCreateClass
import top.nabil.nugazlah.data.remote.response.ResponseGetMyClassesData
import top.nabil.nugazlah.repository.AuthRepository
import top.nabil.nugazlah.repository.ClassRepository
import top.nabil.nugazlah.util.Resource

data class ClassData(
    val id: String,
    val name: String,
    val description: String,
    val lecturer: String,
    val icon: String,
    val code: String,
    val maker: String
)

fun ResponseGetMyClassesData.toClassData(): ClassData {
    return ClassData(
        id = this.id,
        name = this.name,
        description = this.description,
        lecturer = this.lecturer,
        icon = this.icon,
        code = this.code,
        maker = this.maker
    )
}

data class HomeScreenState(
    val classCode: String = "",
    val classLecturer: String = "",
    val className: String = "",
    val classDescription: String = "",
    val classIcon: String = "",
    val isCreateClassLoading: Boolean = false,
    val isDialogOpen: Boolean = false,
    val isDialogJoinRoomOpen: Boolean = false,
    val isDialogMakeRoomOpen: Boolean = false,
    val isDialogLogoutOpen: Boolean = false,
    val classes: List<ClassData> = emptyList()
)

sealed class HomeScreenEvent {
    data class ShowToast(val message: String) : HomeScreenEvent()
}

class HomeViewModel(
    private val classRepository: ClassRepository,
    private val authRepository: AuthRepository,
    private val navController: NavController
) : ViewModel() {
    private val _state = MutableStateFlow(HomeScreenState())
    val state = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<HomeScreenEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    var isGetClassesLoading by mutableStateOf(false)

    init {
        getMyClasses()
    }

    fun getMyClasses() {
        val vm = this
        viewModelScope.launch {
            isGetClassesLoading = true
            when (val result = vm.classRepository.getMyClasses()) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(classes = result.data?.map { newData -> newData.toClassData() }
                            ?: emptyList())
                    }
                }

                is Resource.Error -> {
                    _eventFlow.emit(HomeScreenEvent.ShowToast(result.message!!))
                }
            }
            isGetClassesLoading = false
        }
    }

    fun createMyClass() {
        val vm = this
        val value = _state.value
        viewModelScope.launch {
            isGetClassesLoading = true
            when (val result = vm.classRepository.createClass(
                RequestCreateClass(
                    description = value.classDescription,
                    icon = value.classIcon,
                    lecturer = value.classLecturer,
                    name = value.className
                )
            )) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            classDescription = "",
                            classIcon = "",
                            classLecturer = "",
                            className = "",
                            isDialogMakeRoomOpen = false,
                            isDialogOpen = false
                        )
                    }
                    getMyClasses()
                }

                is Resource.Error -> {
                    _eventFlow.emit(HomeScreenEvent.ShowToast(result.message!!))
                }
            }
            isGetClassesLoading = false
        }
    }

    fun joinClass() {
        val vm = this
        val value = _state.value
        viewModelScope.launch {
            isGetClassesLoading = true
            when (val result = vm.classRepository.joinClass(value.classCode)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            classCode = "",
                            isDialogJoinRoomOpen = false,
                            isDialogOpen = false
                        )
                    }
                    getMyClasses()
                }

                is Resource.Error -> {
                    _eventFlow.emit(HomeScreenEvent.ShowToast(result.message!!))
                }
            }
            isGetClassesLoading = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            _state.update { it.copy(isDialogLogoutOpen = false) }
            authRepository.logout()
            navController.navigate(Screen.LoginScreen) {
                popUpTo(Screen.HomeScreen) { inclusive = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    fun onStateChange(state: HomeScreenState) {
        _state.update { state }
    }
}