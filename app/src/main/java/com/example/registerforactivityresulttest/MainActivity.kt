package com.example.registerforactivityresulttest

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

/*
요구사항
- 다이얼로그 띄우기
- 요청 조건 체크
- 결과 Pass/Fail 판정
- 사전 Permission 목록 정의
- launch 시점 result callback 적용
*/

class HomeFragment : Fragment() {
    private val launcher = DeferredActivityResultLauncher(
        activityResultLauncherProvider = { callback ->
            registerForActivityResultWithRationale(
                rationaleMessage = "권한이 필요해요.",
                onNegativeButtonClick = {
                    callback.onActivityResult(false)
                },
                contract = ConditionalMultiplePermissionRequestContract(
                    resultPredicate = { permissionStateMap -> permissionStateMap.values.all { it } },
                    requestPredicate = { permissionStateMap ->
                        permissionStateMap.values.all { it }.not()
                    }
                ),
                callback
            )
        },
        input = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    )
}

class MainActivity : AppCompatActivity() {

    private val launcher = DeferredActivityResultLauncher(
        activityResultLauncherProvider = { callback ->
            registerForActivityResultWithRationale(
                rationaleMessage = "권한이 필요해요.",
                onNegativeButtonClick = {
                    callback.onActivityResult(false)
                },
                contract = ConditionalMultiplePermissionRequestContract(
                    resultPredicate = { permissionStateMap -> permissionStateMap.values.all { it } },
                    requestPredicate = { permissionStateMap ->
                        permissionStateMap.values.all { it }.not()
                    }
                ),
                callback
            )
        },
        input = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        launcher.launch {
            if (it) {
                Toast.makeText(this, "권한 허용", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "권한 거부", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

fun <O> ComponentActivity.registerForActivityResultWithRationale(
    rationaleMessage: String,
    onNegativeButtonClick: () -> Unit,
    contract: ActivityResultContract<Array<String>, O>,
    callback: ActivityResultCallback<O>
): ActivityResultLauncher<Array<String>> {
    return registerForActivityResultWithRationale(
        context = this,
        shouldShowRequestPermissionRationale = this::shouldShowRequestPermissionRationale,
        rationaleMessage = rationaleMessage,
        onNegativeButtonClick = onNegativeButtonClick,
        contract = contract,
        callback = callback,
        registerActivityForResult = this::registerForActivityResult
    )
}

fun <O> Fragment.registerForActivityResultWithRationale(
    rationaleMessage: String,
    onNegativeButtonClick: () -> Unit,
    contract: ActivityResultContract<Array<String>, O>,
    callback: ActivityResultCallback<O>
): ActivityResultLauncher<Array<String>> {
    return registerForActivityResultWithRationale(
        context = requireContext(),
        shouldShowRequestPermissionRationale = this::shouldShowRequestPermissionRationale,
        rationaleMessage = rationaleMessage,
        onNegativeButtonClick = onNegativeButtonClick,
        contract = contract,
        callback = callback,
        registerActivityForResult = this::registerForActivityResult
    )
}

fun <O> registerForActivityResultWithRationale(
    context: Context,
    shouldShowRequestPermissionRationale: (String) -> Boolean,
    rationaleMessage: String,
    onNegativeButtonClick: () -> Unit,
    contract: ActivityResultContract<Array<String>, O>,
    callback: ActivityResultCallback<O>,
    registerActivityForResult: (ActivityResultContract<Array<String>, O>, ActivityResultCallback<O>) -> ActivityResultLauncher<Array<String>>
): ActivityResultLauncher<Array<String>> {
    return RationaleActivityResultLauncher(
        context = context,
        shouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale,
        rationaleMessage = rationaleMessage,
        onNegativeButtonClick = onNegativeButtonClick,
        launcher = registerActivityForResult(contract, callback)
    )
}

class RationaleActivityResultLauncher(
    private val context: Context,
    private val launcher: ActivityResultLauncher<Array<String>>,
    private val rationaleMessage: String,
    private val shouldShowRequestPermissionRationale: (String) -> Boolean,
    private val onNegativeButtonClick: () -> Unit
) : ActivityResultLauncher<Array<String>>() {
    override val contract: ActivityResultContract<Array<String>, *>
        get() = launcher.contract

    override fun unregister() {
        launcher.unregister()
    }

    override fun launch(input: Array<String>, options: ActivityOptionsCompat?) {
        val permissionsToShowRationale = input.filter { shouldShowRequestPermissionRationale(it) }

        if (permissionsToShowRationale.isNotEmpty()) {
            AlertDialog.Builder(context)
                .setMessage(rationaleMessage)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    launcher.launch(input, options)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    onNegativeButtonClick()
                }
                .show()
        } else {
            launcher.launch(input, options)
        }
    }
}

class ConditionalMultiplePermissionRequestContract(
    private val resultPredicate: (Map<String, Boolean>) -> Boolean,
    private val requestPredicate: (Map<String, Boolean>) -> Boolean = resultPredicate
) : ActivityResultContract<Array<String>, Boolean>() {

    private val multiplePermissionRequestContract =
        ActivityResultContracts.RequestMultiplePermissions()

    override fun createIntent(context: Context, input: Array<String>): Intent {
        return multiplePermissionRequestContract.createIntent(context, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultPredicate(multiplePermissionRequestContract.parseResult(resultCode, intent))
    }

    override fun getSynchronousResult(
        context: Context,
        input: Array<String>
    ): SynchronousResult<Boolean>? {
        val permissionStateMap = input.associateWith {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        return if (requestPredicate(permissionStateMap)) {
            SynchronousResult(true)
        } else null
    }
}

class DeferredActivityResultLauncher<I, O>(
    private val input: I,
    activityResultLauncherProvider: (ActivityResultCallback<O>) -> ActivityResultLauncher<I>,
) {
    private var mCallback: ((O) -> Unit)? = null

    private val launcher = activityResultLauncherProvider {
        mCallback?.invoke(it)
    }

    val contract: ActivityResultContract<I, *>
        get() = launcher.contract

    fun unregister() {
        launcher.unregister()
    }

    fun launch(options: ActivityOptionsCompat? = null, callback: (O) -> Unit) {
        mCallback = callback
        launcher.launch(input, options)
    }
}
