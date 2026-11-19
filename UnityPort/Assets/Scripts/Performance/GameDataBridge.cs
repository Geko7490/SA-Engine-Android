// SPDX-License-Identifier: MIT
// Unity Android game side: read launcher intent extras and await private game data transfer.
using System;
using UnityEngine;

namespace SAEngineAndroid.Performance
{
    public static class GameDataBridge
    {
        private static bool started;
#if UNITY_ANDROID && !UNITY_EDITOR
        private static AndroidJavaClass receiver;

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.BeforeSceneLoad)]
        public static void Begin()
        {
            if (started) return;
            started = true;
            try
            {
                using (var unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
                using (var activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity"))
                using (var intent = activity.Call<AndroidJavaObject>("getIntent"))
                {
                    int requestedFps = intent.Call<int>("getIntExtra",
                        "com.geko7490.saenginegame.extra.FPS", 60);
                    int requestedQuality = intent.Call<int>("getIntExtra",
                        "com.geko7490.saenginegame.extra.QUALITY", 1);
                    requestedFps = requestedFps <= 30 ? 30 :
                        requestedFps <= 60 ? 60 : requestedFps <= 90 ? 90 : 120;
                    PlayerPrefs.SetInt("SA_ANDROID_TARGET_FPS", requestedFps);
                    PlayerPrefs.SetInt("SA_ANDROID_QUALITY", Mathf.Clamp(requestedQuality, 0, 2));
                    PlayerPrefs.Save();
                    UnityEngine.QualitySettings.vSyncCount = 0;
                    Application.targetFrameRate = requestedFps;

                    receiver = new AndroidJavaClass("com.geko7490.saenginegame.GameDataReceiver");
                    receiver.CallStatic("start", activity);
                }
            }
            catch (Exception ex)
            {
                error = "Android bridge initialization: " + ex.Message;
            }
        }

        private static string error = "";
        public static bool IsBusy
        {
            get
            {
                if (!started) Begin();
                return receiver != null && receiver.CallStatic<bool>("isBusy");
            }
        }
        public static string Error
        {
            get
            {
                if (!string.IsNullOrEmpty(error)) return error;
                if (receiver == null) return "";
                return receiver.CallStatic<string>("getError") ?? "";
            }
        }
#else
        public static void Begin() { started = true; }
        public static bool IsBusy { get { return false; } }
        public static string Error { get { return ""; } }
#endif
    }
}
