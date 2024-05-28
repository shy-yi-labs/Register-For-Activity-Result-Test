package com.example.registerforactivityresulttest

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        Toast.makeText(this, it.toString(), Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        launcher.launch(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        )
    }
}

fun <I, O> ComponentActivity.registerForActivityResultDelegate(
    contract: ActivityResultContract<I, O>,
    callback: ActivityResultCallback<O>
): ActivityResultLauncher<I> {
    return ActivityResultRegisterDelegate(
        context = this,
        shouldShowRequestPermissionRationale = this::shouldShowRequestPermissionRationale,
        contract = contract,
        callback = callback,
        registerActivityForResult = this::registerForActivityResult
    ).register()
}

fun <I, O> Fragment.registerForActivityResultDelegate(
    contract: ActivityResultContract<I, O>,
    callback: ActivityResultCallback<O>
): ActivityResultLauncher<I> {
    return ActivityResultRegisterDelegate(
        context = requireContext(),
        shouldShowRequestPermissionRationale = this::shouldShowRequestPermissionRationale,
        contract = contract,
        callback = callback,
        registerActivityForResult = this::registerForActivityResult
    ).register()
}

class ActivityResultRegisterDelegate<I, O>(
    private val context: Context,
    private val shouldShowRequestPermissionRationale: (String) -> Boolean,
    private val contract: ActivityResultContract<I, O>,
    private val callback: ActivityResultCallback<O>,
    private val registerActivityForResult: (ActivityResultContract<I, O>, ActivityResultCallback<O>) -> ActivityResultLauncher<I>
) {

    fun register(): ActivityResultLauncher<I> {
        return registerActivityForResult(contract, callback)
    }
}
