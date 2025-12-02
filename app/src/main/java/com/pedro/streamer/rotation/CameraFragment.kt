/*
 * Copyright (C) 2024 pedroSG94.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pedro.streamer.rotation

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.sources.video.Camera1Source
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.extrasources.CameraXSource
import com.pedro.library.base.recording.RecordController
import com.pedro.library.generic.GenericStream
import com.pedro.library.util.BitrateAdapter
import com.pedro.streamer.R
import com.pedro.streamer.utils.PathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Example code to stream using StreamBase. This is the recommend way to use the library.
 * Necessary API 21+
 * This mode allow you stream using custom Video/Audio sources, attach a preview or not dynamically, support device rotation, etc.
 *
 * Check Menu to use filters, video and audio sources, and orientation
 *
 * Orientation horizontal (by default) means that you want stream with vertical resolution
 * (with = 640, height = 480 and rotation = 0) The stream/record result will be 640x480 resolution
 *
 * Orientation vertical means that you want stream with vertical resolution
 * (with = 640, height = 480 and rotation = 90) The stream/record result will be 480x640 resolution
 *
 * More documentation see:
 * [com.pedro.library.base.StreamBase]
 * Support RTMP, RTSP and SRT with commons features
 * [com.pedro.library.generic.GenericStream]
 * Support RTSP with all RTSP features
 * [com.pedro.library.rtsp.RtspStream]
 * Support RTMP with all RTMP features
 * [com.pedro.library.rtmp.RtmpStream]
 * Support SRT with all SRT features
 * [com.pedro.library.srt.SrtStream]
 */

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class CameraFragment: Fragment(), ConnectChecker {

  companion object {
    private const val TAG = "CameraFragment"
    fun getInstance(): CameraFragment {
      Log.d(TAG, "getInstance被调用")
      return CameraFragment()
    }
  }

  private var _genericStream: GenericStream? = null
  val genericStream: GenericStream
    get() {
      if (_genericStream == null) {
        Log.d(TAG, "初始化GenericStream")
        try {
          _genericStream = GenericStream(requireContext(), this).apply {
            getGlInterface().autoHandleOrientation = true
            getStreamClient().setBitrateExponentialFactor(0.5f)
          }
          Log.d(TAG, "GenericStream初始化成功")
        } catch (e: Exception) {
          Log.e(TAG, "GenericStream初始化失败", e)
          throw e
        }
      }
      return _genericStream!!
    }

  private lateinit var surfaceView: SurfaceView
  private lateinit var bStartStop: ImageView
  private lateinit var txtBitrate: TextView
  val width = 640
  val height = 480
  val vBitrate = 1200 * 1000
  private var rotation = 0
  private val sampleRate = 32000
  private val isStereo = true
  private val aBitrate = 128 * 1000
  private var recordPath = ""
  private var isPrepared = false // 标记是否已完成prepare
  private var surfaceReady = false // 标记Surface是否已准备好

  //Bitrate adapter used to change the bitrate on fly depend of the bandwidth.
  private val bitrateAdapter = BitrateAdapter {
    genericStream.setVideoBitrateOnFly(it)
  }.apply {
    setMaxBitrate(vBitrate + aBitrate)
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View? {
    Log.d(TAG, "onCreateView开始")
    try {
      val view = inflater.inflate(R.layout.fragment_camera, container, false)
      Log.d(TAG, "布局加载成功")

      Log.d(TAG, "开始查找视图组件")
      bStartStop = view.findViewById(R.id.b_start_stop)
      Log.d(TAG, "bStartStop找到: ${bStartStop != null}")
      val bRecord = view.findViewById<ImageView>(R.id.b_record)
      Log.d(TAG, "bRecord找到: ${bRecord != null}")
      val bSwitchCamera = view.findViewById<ImageView>(R.id.switch_camera)
      Log.d(TAG, "bSwitchCamera找到: ${bSwitchCamera != null}")
      val etUrl = view.findViewById<EditText>(R.id.et_rtp_url)
      Log.d(TAG, "etUrl找到: ${etUrl != null}")
      txtBitrate = view.findViewById(R.id.txt_bitrate)
      Log.d(TAG, "txtBitrate找到: ${txtBitrate != null}")
      surfaceView = view.findViewById(R.id.surfaceView)
      Log.d(TAG, "surfaceView找到: ${surfaceView != null}")

      (activity as? RotationActivity)?.let {
        Log.d(TAG, "设置SurfaceView触摸监听器")
        surfaceView.setOnTouchListener(it)
      } ?: Log.w(TAG, "Activity为null，无法设置触摸监听器")

      Log.d(TAG, "设置SurfaceHolder回调")
      surfaceView.holder.addCallback(object: SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
          Log.d(TAG, "Surface创建")
          surfaceReady = true
          // 只有在prepare完成后才启动预览
          if (isPrepared && !genericStream.isOnPreview) {
            Log.d(TAG, "prepare已完成，开始预览")
            startPreviewIfReady()
          } else {
            Log.d(TAG, "等待prepare完成，当前isPrepared=$isPrepared, isOnPreview=${genericStream.isOnPreview}")
          }
        }
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
          Log.d(TAG, "Surface改变: ${width}x${height}, format=$format")
          if (isPrepared) {
            try {
              genericStream.getGlInterface().setPreviewResolution(width, height)
            } catch (e: Exception) {
              Log.e(TAG, "设置预览分辨率失败", e)
            }
          }
        }
        override fun surfaceDestroyed(holder: SurfaceHolder) {
          Log.d(TAG, "Surface销毁")
          surfaceReady = false
          try {
            if (genericStream.isOnPreview) genericStream.stopPreview()
          } catch (e: Exception) {
            Log.e(TAG, "停止预览失败", e)
          }
        }
      })
      Log.d(TAG, "SurfaceHolder回调设置完成")

      Log.d(TAG, "设置按钮点击监听器")
      bStartStop.setOnClickListener {
        Log.d(TAG, "开始/停止按钮被点击")
        try {
          if (!genericStream.isStreaming) {
            Log.d(TAG, "开始流媒体，URL: ${etUrl.text}")
            genericStream.startStream(etUrl.text.toString())
            bStartStop.setImageResource(R.drawable.stream_stop_icon)
          } else {
            Log.d(TAG, "停止流媒体")
            genericStream.stopStream()
            bStartStop.setImageResource(R.drawable.stream_icon)
          }
        } catch (e: Exception) {
          Log.e(TAG, "流媒体操作失败", e)
        }
      }

      bRecord.setOnClickListener {
        Log.d(TAG, "录制按钮被点击")
        try {
          if (!genericStream.isRecording) {
            val folder = PathUtils.getRecordPath()
            if (!folder.exists()) folder.mkdir()
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            recordPath = "${folder.absolutePath}/${sdf.format(Date())}.mp4"
            Log.d(TAG, "开始录制，路径: $recordPath")
            genericStream.startRecord(recordPath) { status ->
              Log.d(TAG, "录制状态: $status")
              if (status == RecordController.Status.RECORDING) {
                bRecord.setImageResource(R.drawable.stop_icon)
              }
            }
            bRecord.setImageResource(R.drawable.pause_icon)
          } else {
            Log.d(TAG, "停止录制")
            genericStream.stopRecord()
            bRecord.setImageResource(R.drawable.record_icon)
            PathUtils.updateGallery(requireContext(), recordPath)
          }
        } catch (e: Exception) {
          Log.e(TAG, "录制操作失败", e)
        }
      }

      bSwitchCamera.setOnClickListener {
        Log.d(TAG, "切换摄像头按钮被点击")
        try {
          when (val source = genericStream.videoSource) {
            is Camera1Source -> {
              Log.d(TAG, "使用Camera1Source切换摄像头")
              source.switchCamera()
            }
            is Camera2Source -> {
              Log.d(TAG, "使用Camera2Source切换摄像头")
              source.switchCamera()
            }
            is CameraXSource -> {
              Log.d(TAG, "使用CameraXSource切换摄像头")
              source.switchCamera()
            }
            else -> Log.w(TAG, "未知的视频源类型")
          }
        } catch (e: Exception) {
          Log.e(TAG, "切换摄像头失败", e)
        }
      }

      Log.d(TAG, "onCreateView完成")
      return view
    } catch (e: Exception) {
      Log.e(TAG, "onCreateView失败", e)
      return null
    }
  }

  fun setOrientationMode(isVertical: Boolean) {
    val wasOnPreview = genericStream.isOnPreview
    val stream = genericStream
    try {
      if (stream.isOnPreview) {
        stream.stopPreview()
      }
      if (stream.isStreaming) {
        stream.stopStream()
      }
      if (stream.isRecording) {
        stream.stopRecord()
      }
      stream.release()
    } catch (e: Exception) {
      Log.e(TAG, "释放stream失败", e)
    }
    rotation = if (isVertical) 90 else 0
    isPrepared = false
    lifecycleScope.launch {
      prepare(stream)
      if (wasOnPreview && isPrepared) {
        withContext(Dispatchers.Main) {
          startPreviewIfReady()
        }
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    Log.d(TAG, "onCreate开始")
    super.onCreate(savedInstanceState)
    Log.d(TAG, "onCreate完成")
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    Log.d(TAG, "onViewCreated开始")
    super.onViewCreated(view, savedInstanceState)
    Log.d(TAG, "开始异步初始化流程")
    // 延迟初始化，避免阻塞主线程
    // 先初始化genericStream（在主线程），然后在后台线程执行prepare
    lifecycleScope.launch {
      try {
        Log.d(TAG, "协程启动，准备初始化GenericStream")
        // 在主线程初始化genericStream
        val stream = withContext(Dispatchers.Main) {
          Log.d(TAG, "在主线程初始化GenericStream")
          try {
            val s = genericStream
            Log.d(TAG, "GenericStream获取成功，设置重试次数")
            s.getStreamClient().setReTries(10)
            Log.d(TAG, "重试次数设置完成")
            s
          } catch (e: Exception) {
            Log.e(TAG, "在主线程初始化GenericStream失败", e)
            throw e
          }
        }
        Log.d(TAG, "GenericStream初始化完成，开始prepare")
        // 在后台线程执行prepare
        prepare(stream)
      } catch (e: Exception) {
        Log.e(TAG, "异步初始化流程失败", e)
      }
    }
    Log.d(TAG, "onViewCreated完成")
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

  private suspend fun prepare(stream: GenericStream) {
    Log.d(TAG, "prepare开始，参数: width=$width, height=$height, vBitrate=$vBitrate, rotation=$rotation")
    
    // 确保在prepare之前停止所有操作
    withContext(Dispatchers.Main) {
      try {
        Log.d(TAG, "检查并停止现有操作")
        if (stream.isOnPreview) {
          Log.d(TAG, "停止预览")
          stream.stopPreview()
        }
        if (stream.isStreaming) {
          Log.d(TAG, "停止流媒体")
          stream.stopStream()
        }
        if (stream.isRecording) {
          Log.d(TAG, "停止录制")
          stream.stopRecord()
        }
        Log.d(TAG, "所有操作已停止")
      } catch (e: Exception) {
        Log.w(TAG, "停止操作时出现异常（可能没有运行的操作）", e)
      }
    }
    
    val prepared = withContext(Dispatchers.IO) {
      try {
        Log.d(TAG, "在IO线程开始准备视频和音频配置...")
        Log.d(TAG, "准备视频: ${width}x${height}, bitrate=$vBitrate, rotation=$rotation")
        val videoPrepared = stream.prepareVideo(width, height, vBitrate, rotation = rotation)
        Log.d(TAG, "视频准备结果: $videoPrepared")
        
        Log.d(TAG, "准备音频: sampleRate=$sampleRate, isStereo=$isStereo, bitrate=$aBitrate")
        val audioPrepared = stream.prepareAudio(sampleRate, isStereo, aBitrate)
        Log.d(TAG, "音频准备结果: $audioPrepared")
        
        val result = videoPrepared && audioPrepared
        Log.d(TAG, "准备配置最终结果: $result")
        result
      } catch (e: Exception) {
        Log.e(TAG, "准备配置失败", e)
        e.printStackTrace()
        false
      }
    }
    
    if (!prepared) {
      Log.e(TAG, "配置准备失败，准备关闭Activity")
      withContext(Dispatchers.Main) {
        try {
          PathUtils.toast(requireContext(), "Audio or Video configuration failed")
          activity?.finish()
        } catch (e: Exception) {
          Log.e(TAG, "关闭Activity失败", e)
        }
      }
    } else {
      Log.d(TAG, "配置准备完成，可以开始使用")
      isPrepared = true
      // 如果Surface已经准备好，立即启动预览
      withContext(Dispatchers.Main) {
        if (surfaceReady) {
          Log.d(TAG, "Surface已准备好，启动预览")
          startPreviewIfReady()
        } else {
          Log.d(TAG, "Surface尚未准备好，等待Surface创建")
        }
      }
    }
  }
  
  private fun startPreviewIfReady() {
    if (!isPrepared) {
      Log.w(TAG, "prepare尚未完成，无法启动预览")
      return
    }
    if (!surfaceReady) {
      Log.w(TAG, "Surface尚未准备好，无法启动预览")
      return
    }
    try {
      if (!genericStream.isOnPreview) {
        Log.d(TAG, "启动预览")
        genericStream.startPreview(surfaceView)
        Log.d(TAG, "预览启动成功")
      } else {
        Log.d(TAG, "预览已在进行中")
      }
    } catch (e: Exception) {
      Log.e(TAG, "启动预览失败", e)
    }
  }

  override fun onDestroy() {
    Log.d(TAG, "onDestroy开始")
    super.onDestroy()
    try {
      genericStream.release()
      Log.d(TAG, "GenericStream已释放")
    } catch (e: Exception) {
      Log.e(TAG, "释放GenericStream失败", e)
    }
    isPrepared = false
    surfaceReady = false
    Log.d(TAG, "onDestroy完成")
  }

  override fun onConnectionStarted(url: String) {
  }

  override fun onConnectionSuccess() {
    PathUtils.toast(requireContext(), "Connected")
  }

  override fun onConnectionFailed(reason: String) {
    if (genericStream.getStreamClient().reTry(5000, reason, null)) {
      PathUtils.toast(requireContext(), "Retry")
    } else {
      genericStream.stopStream()
      bStartStop.setImageResource(R.drawable.stream_icon)
      PathUtils.toast(requireContext(), "Failed: $reason")
    }
  }

  override fun onNewBitrate(bitrate: Long) {
    bitrateAdapter.adaptBitrate(bitrate, genericStream.getStreamClient().hasCongestion())
    txtBitrate.text = String.format(Locale.getDefault(), "%.1f mb/s", bitrate / 1000_000f)
  }

  override fun onDisconnect() {
    txtBitrate.text = String()
    PathUtils.toast(requireContext(), "Disconnected")
  }

  override fun onAuthError() {
    genericStream.stopStream()
    bStartStop.setImageResource(R.drawable.stream_icon)
    PathUtils.toast(requireContext(), "Auth error")
  }

  override fun onAuthSuccess() {
    PathUtils.toast(requireContext(), "Auth success")
  }
}
