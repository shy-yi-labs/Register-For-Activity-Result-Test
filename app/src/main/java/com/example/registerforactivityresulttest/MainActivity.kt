package com.example.registerforactivityresulttest

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
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
    private val launcher = registerReadExternalMediaPermissionRequest()
}

class MainActivity : AppCompatActivity() {

    private val launcher = registerReadExternalMediaPermissionRequest()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val button = findViewById<Button>(R.id.btClickMe)

        button.setOnClickListener {
            launcher.launch {
                if (it) {
                    Toast.makeText(this, "권한 허용", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "권한 거부", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

fun ComponentActivity.registerReadExternalMediaPermissionRequest(): PredefinedActivityResultLauncher<Array<String>, Boolean> {
    return registerReadExternalMediaPermissionRequest(
        context = this,
        registerForActivityResult = this::registerForActivityResult,
        shouldShowRequestPermissionRationale = this::shouldShowRequestPermissionRationale
    )
}

fun Fragment.registerReadExternalMediaPermissionRequest(): PredefinedActivityResultLauncher<Array<String>, Boolean> {
    return registerReadExternalMediaPermissionRequest(
        context = requireContext(),
        registerForActivityResult = this::registerForActivityResult,
        shouldShowRequestPermissionRationale = this::shouldShowRequestPermissionRationale
    )
}

fun registerReadExternalMediaPermissionRequest(
    context: Context,
    registerForActivityResult: (ActivityResultContract<Array<String>, Boolean>, ActivityResultCallback<Boolean>) -> ActivityResultLauncher<Array<String>>,
    shouldShowRequestPermissionRationale: (String) -> Boolean
): PredefinedActivityResultLauncher<Array<String>, Boolean> {
    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    return PredefinedActivityResultLauncher(
        activityResultLauncherProvider = { callback ->
            registerForActivityResultWithRationale(
                context = context,
                registerForActivityResult = registerForActivityResult,
                rationaleMessage = "권한이 필요해요.",
                shouldShowRequestPermissionRationale = { _ ->  true},
                onNegativeButtonClick = {
                    callback.onActivityResult(false)
                },
                contract = ConditionalMultiplePermissionRequestContract(
                    resultPredicate = { permissionStateMap -> permissionStateMap.values.all { it } },
                    requestPredicate = { permissionStateMap ->
                        permissionStateMap.values.all { it }.not()
                    }
                ),
                callback = PermissionPermanentlyDeniedActivityLauncherCallback(
                    context = context,
                    callback = callback,
                    permissions = permissions,
                    shouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale
                )
            )
        },
        input = permissions
    )
}

class PermissionPermanentlyDeniedActivityLauncherCallback<O>(
    val context: Context,
    val callback: ActivityResultCallback<O>,
    val permissions: Array<String>,
    val shouldShowRequestPermissionRationale: (String) -> Boolean
): ActivityResultCallback<O> {
    override fun onActivityResult(result: O) {
        val permanentlyDeniedPermissions = permissions.filter {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_DENIED &&
                    shouldShowRequestPermissionRationale(it)
        }

        val message = StringBuilder().apply {
            append("해당 기능을 사용할 수 없습니다.\n권한을 허용하시려면 설정을 눌러주세요.\n\n필요 권한:")
            permanentlyDeniedPermissions.forEach {
                append("\n- $it")
            }
        }

        if (permanentlyDeniedPermissions.isNotEmpty()) {
            AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()
            return
        }

        callback.onActivityResult(result)
    }
}


fun <O> ComponentActivity.registerForActivityResultWithRationale(
    rationaleMessage: String,
    shouldShowRequestPermissionRationale: (String) -> Boolean = this::shouldShowRequestPermissionRationale,
    onNegativeButtonClick: () -> Unit,
    contract: ActivityResultContract<Array<String>, O>,
    callback: ActivityResultCallback<O>
): ActivityResultLauncher<Array<String>> {
    return registerForActivityResultWithRationale(
        context = this,
        registerForActivityResult = this::registerForActivityResult,
        shouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale,
        rationaleMessage = rationaleMessage,
        onNegativeButtonClick = onNegativeButtonClick,
        contract = contract,
        callback = callback
    )
}

fun <O> Fragment.registerForActivityResultWithRationale(
    rationaleMessage: String,
    shouldShowRequestPermissionRationale: (String) -> Boolean = this::shouldShowRequestPermissionRationale,
    onNegativeButtonClick: () -> Unit,
    contract: ActivityResultContract<Array<String>, O>,
    callback: ActivityResultCallback<O>
): ActivityResultLauncher<Array<String>> {
    return registerForActivityResultWithRationale(
        context = requireContext(),
        registerForActivityResult = this::registerForActivityResult,
        shouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale,
        rationaleMessage = rationaleMessage,
        onNegativeButtonClick = onNegativeButtonClick,
        contract = contract,
        callback = callback
    )
}

fun <O> registerForActivityResultWithRationale(
    context: Context,
    registerForActivityResult: (ActivityResultContract<Array<String>, O>, ActivityResultCallback<O>) -> ActivityResultLauncher<Array<String>>,
    shouldShowRequestPermissionRationale: (String) -> Boolean,
    rationaleMessage: String,
    onNegativeButtonClick: () -> Unit,
    contract: ActivityResultContract<Array<String>, O>,
    callback: ActivityResultCallback<O>
): ActivityResultLauncher<Array<String>> {
    return RationaleActivityResultLauncher(
        context = context,
        shouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale,
        rationaleMessage = rationaleMessage,
        onNegativeButtonClick = onNegativeButtonClick,
        launcher = registerForActivityResult(contract, callback)
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
            null
        } else {
            SynchronousResult(true)
        }
    }
}

class PredefinedActivityResultLauncher<I, O>(
    private val input: I,
    activityResultLauncherProvider: (ActivityResultCallback<O>) -> ActivityResultLauncher<I>,
) {
    private var mCallback: ((O) -> Unit)? = null

    private val launcher = activityResultLauncherProvider {
        mCallback?.invoke(it)
        mCallback = null
    }

    val contract: ActivityResultContract<I, *>
        get() = launcher.contract

    fun unregister() {
        launcher.unregister()
    }

    @Synchronized
    fun launch(options: ActivityOptionsCompat? = null, callback: (O) -> Unit) {
        if (mCallback == null) {
            mCallback = callback
        } else {
            throw IllegalStateException("Already waiting for a result")
        }
        launcher.launch(input, options)
    }
}
