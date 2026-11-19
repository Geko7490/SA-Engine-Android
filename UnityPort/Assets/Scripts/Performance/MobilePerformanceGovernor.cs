// SPDX-License-Identifier: MIT
// Original code for SA-Engine-Android. Not part of upstream SanAndreasUnity.
using UnityEngine;

namespace SAEngineAndroid.Performance
{
    /// <summary>
    /// Optional frame-pacing and GPU-load governor for a Unity Android port.
    /// Attach once to a persistent GameObject. Does not alter core gameplay logic.
    /// Uses measured frame times, not a claim of guaranteed FPS improvement.
    /// </summary>
    public sealed class MobilePerformanceGovernor : MonoBehaviour
    {
        public enum TargetMode { Fps30 = 30, Fps60 = 60, Fps90 = 90, Fps120 = 120 }

        [Header("Frame pacing")]
        [SerializeField] private TargetMode targetMode = TargetMode.Fps60;
        [SerializeField] private bool disableVSync = true;
        [SerializeField] private float sampleWindowSeconds = 2f;

        [Header("Optional shadow governor")]
        [SerializeField] private bool adaptShadowDistance = true;
        [SerializeField] private float minShadowDistance = 25f;
        [SerializeField] private float maxShadowDistance = 85f;
        [SerializeField] private float shadowStep = 5f;
        [SerializeField] private float changeCooldownSeconds = 5f;

        [Header("Read-only diagnostics")]
        [SerializeField] private float sampledFps;
        [SerializeField] private float sampledFrameMs;

        private float elapsed;
        private int frames;
        private float nextChangeTime;
        private int overBudgetWindows;
        private int underBudgetWindows;
        private int savedVSync;
        private int savedTargetFrameRate;
        private float savedShadowDistance;

        public float SampledFps { get { return sampledFps; } }
        public float SampledFrameMs { get { return sampledFrameMs; } }

        private void OnEnable()
        {
            savedVSync = QualitySettings.vSyncCount;
            savedTargetFrameRate = Application.targetFrameRate;
            savedShadowDistance = QualitySettings.shadowDistance;
            int storedFps = PlayerPrefs.GetInt("SA_ANDROID_TARGET_FPS", 60);
            SetTargetFps(storedFps, false);
            elapsed = 0f;
            frames = 0;
            overBudgetWindows = 0;
            underBudgetWindows = 0;
            nextChangeTime = Time.unscaledTime + changeCooldownSeconds;
        }

        private void OnDisable()
        {
            QualitySettings.vSyncCount = savedVSync;
            Application.targetFrameRate = savedTargetFrameRate;
            if (adaptShadowDistance) QualitySettings.shadowDistance = savedShadowDistance;
        }

        public void SetTargetFps(int fps)
        {
            SetTargetFps(fps, true);
        }

        private void SetTargetFps(int fps, bool persist)
        {
            if (fps <= 30) targetMode = TargetMode.Fps30;
            else if (fps <= 60) targetMode = TargetMode.Fps60;
            else if (fps <= 90) targetMode = TargetMode.Fps90;
            else targetMode = TargetMode.Fps120;
            if (persist)
            {
                PlayerPrefs.SetInt("SA_ANDROID_TARGET_FPS", (int)targetMode);
                PlayerPrefs.Save();
            }
            ApplyTarget();
        }

        private void ApplyTarget()
        {
            if (disableVSync) QualitySettings.vSyncCount = 0;
            Application.targetFrameRate = (int)targetMode;
        }

        private void Update()
        {
            float dt = Time.unscaledDeltaTime;
            if (dt <= 0f || dt > 1f) return;
            elapsed += dt;
            frames++;
            if (elapsed < Mathf.Max(0.5f, sampleWindowSeconds)) return;

            sampledFps = frames / elapsed;
            sampledFrameMs = 1000f * elapsed / frames;
            elapsed = 0f;
            frames = 0;

            if (!adaptShadowDistance || Time.unscaledTime < nextChangeTime) return;
            float budgetMs = 1000f / (int)targetMode;
            // Hysteresis: three slow windows to lower quality, five fast windows to raise it.
            if (sampledFrameMs > budgetMs * 1.12f)
            {
                overBudgetWindows++;
                underBudgetWindows = 0;
            }
            else if (sampledFrameMs < budgetMs * 0.75f)
            {
                underBudgetWindows++;
                overBudgetWindows = 0;
            }
            else
            {
                overBudgetWindows = 0;
                underBudgetWindows = 0;
            }

            float low = Mathf.Min(minShadowDistance, maxShadowDistance);
            float high = Mathf.Max(minShadowDistance, maxShadowDistance);
            if (overBudgetWindows >= 3)
            {
                QualitySettings.shadowDistance = Mathf.Clamp(
                    QualitySettings.shadowDistance - Mathf.Max(1f, shadowStep), low, high);
                overBudgetWindows = 0;
                nextChangeTime = Time.unscaledTime + Mathf.Max(1f, changeCooldownSeconds);
            }
            else if (underBudgetWindows >= 5)
            {
                QualitySettings.shadowDistance = Mathf.Clamp(
                    QualitySettings.shadowDistance + Mathf.Max(1f, shadowStep), low, high);
                underBudgetWindows = 0;
                nextChangeTime = Time.unscaledTime + Mathf.Max(1f, changeCooldownSeconds);
            }
        }
    }
}
