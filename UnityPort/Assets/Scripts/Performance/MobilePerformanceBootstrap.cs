// SPDX-License-Identifier: MIT
// Original Android performance overlay for the SanAndreasUnity project.
using UnityEngine;
namespace SAEngineAndroid.Performance
{
    public static class MobilePerformanceBootstrap
    {
        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
        private static void Install()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            if (Object.FindObjectOfType<MobilePerformanceGovernor>() != null) return;
            var host = new GameObject("SA Android Performance Governor");
            Object.DontDestroyOnLoad(host);
            host.AddComponent<MobilePerformanceGovernor>();
#endif
        }
    }
}
