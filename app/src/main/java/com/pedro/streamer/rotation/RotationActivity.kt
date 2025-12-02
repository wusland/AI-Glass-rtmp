package com.pedro.streamer.rotation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.pedro.streamer.R

class RotationActivity : AppCompatActivity(), OnTouchListener {

    companion object {
        private const val TAG = "RotationActivity"
    }

    private val cameraFragment by lazy { 
        Log.d(TAG, "创建CameraFragment实例")
        CameraFragment.getInstance() 
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "权限请求回调，权限结果: $permissions")
        
        // 核心权限：必须授予
        val corePermissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        
        // 检查核心权限是否都已授予
        val coreGranted = corePermissions.all { permissions[it] == true }
        Log.d(TAG, "核心权限是否已授予: $coreGranted")
        
        // 检查可选权限状态
        val optionalPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        optionalPermissions.forEach { permission ->
            val granted = permissions[permission] == true
            Log.d(TAG, "可选权限 $permission: ${if (granted) "已授予" else "未授予（不影响功能）"}")
        }
        
        if (coreGranted) {
            Log.d(TAG, "核心权限已授予，开始添加Fragment")
            try {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, cameraFragment)
                    .commit()
                Log.d(TAG, "Fragment添加成功")
            } catch (e: Exception) {
                Log.e(TAG, "添加Fragment失败", e)
            }
        } else {
            Log.w(TAG, "核心权限未全部授予，无法继续")
            // 可以显示一个提示，告知用户需要授予权限
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate开始")
        super.onCreate(savedInstanceState)
        Log.d(TAG, "调用setContentView")
        try {
            setContentView(R.layout.activity_rotation)
            Log.d(TAG, "setContentView成功")
        } catch (e: Exception) {
            Log.e(TAG, "setContentView失败", e)
            return
        }

        Log.d(TAG, "检查权限")
        val hasPermissions = checkPermissions()
        Log.d(TAG, "权限检查结果: $hasPermissions")
        
        if (hasPermissions) {
            Log.d(TAG, "权限已授予，开始添加Fragment")
            try {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, cameraFragment)
                    .commit()
                Log.d(TAG, "Fragment事务提交成功")
            } catch (e: Exception) {
                Log.e(TAG, "添加Fragment失败", e)
            }
        } else {
            Log.d(TAG, "权限未授予，请求权限")
            requestPermissions()
        }
        Log.d(TAG, "onCreate完成")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_rotation, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.orientation_horizontal -> {
                cameraFragment.setOrientationMode(false)
                true
            }
            R.id.orientation_vertical -> {
                cameraFragment.setOrientationMode(true)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return false
    }

    private fun checkPermissions(): Boolean {
        // 核心权限：必须授予才能使用应用
        val corePermissions = mutableListOf<String>()
        corePermissions.add(Manifest.permission.CAMERA)
        corePermissions.add(Manifest.permission.RECORD_AUDIO)
        
        // 可选权限：不影响核心功能
        val optionalPermissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            optionalPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            optionalPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        Log.d(TAG, "需要检查的核心权限: $corePermissions")
        Log.d(TAG, "可选权限: $optionalPermissions")
        
        // 只检查核心权限
        val result = corePermissions.all { permission ->
            val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "核心权限 $permission: ${if (granted) "已授予" else "未授予"}")
            granted
        }
        
        // 记录可选权限状态（不影响结果）
        optionalPermissions.forEach { permission ->
            val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "可选权限 $permission: ${if (granted) "已授予" else "未授予"}")
        }
        
        Log.d(TAG, "权限检查最终结果: $result")
        return result
    }

    private fun requestPermissions() {
        // 请求所有权限（包括可选的），但只要求核心权限必须授予
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        Log.d(TAG, "请求权限: ${permissions.contentToString()}")
        Log.d(TAG, "注意：只有CAMERA和RECORD_AUDIO是必需的，其他权限可选")
        requestPermissionLauncher.launch(permissions)
    }
}
