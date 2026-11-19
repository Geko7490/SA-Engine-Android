// SPDX-License-Identifier: MIT
using SanAndreasUnity.Behaviours;
using SanAndreasUnity.Behaviours.World;
using UnityEngine;

namespace SAEngineAndroid.Performance
{
    // Android-only conservative world streaming distance tuner.
    // Does not change object streaming algorithms; changes the existing engine's distance limit.
    public sealed class MobileWorldTuner : MonoBehaviour
    {
        private const string QualityKey = "SA_ANDROID_QUALITY";
        private MobilePerformanceGovernor governor;
        private WorldManager currentWorld;
        private float nextChange;
        private int slowSamples;
        private int fastSamples;
        private int profile = 1;
        private float minDistance = 350f;
        private float maxDistance = 1000f;
        private float startDistance = 650f;

        public int QualityProfile { get { return profile; } }
        public float CurrentDrawDistance
        {
            get { return currentWorld != null ? currentWorld.MaxDrawDistance : 0f; }
        }

        private void Awake()
        {
            governor = GetComponent<MobilePerformanceGovernor>();
            ApplyProfile(PlayerPrefs.GetInt(QualityKey, 1), false);
        }

        public void ApplyProfile(int value)
        {
            ApplyProfile(value, true);
        }

        private void ApplyProfile(int value, bool persist)
        {
            profile = Mathf.Clamp(value, 0, 2);
            switch (profile)
            {
                case 0:
                    minDistance = 250f;
                    startDistance = 450f;
                    maxDistance = 700f;
                    UnityEngine.QualitySettings.masterTextureLimit = 2;
                    UnityEngine.QualitySettings.antiAliasing = 0;
                    UnityEngine.QualitySettings.shadowDistance = 25f;
                    break;
                case 1:
                    minDistance = 350f;
                    startDistance = 650f;
                    maxDistance = 1000f;
                    UnityEngine.QualitySettings.masterTextureLimit = 1;
                    UnityEngine.QualitySettings.antiAliasing = 0;
                    UnityEngine.QualitySettings.shadowDistance = 50f;
                    break;
                default:
                    minDistance = 500f;
                    startDistance = 850f;
                    maxDistance = 1350f;
                    UnityEngine.QualitySettings.masterTextureLimit = 0;
                    UnityEngine.QualitySettings.antiAliasing = 2;
                    UnityEngine.QualitySettings.shadowDistance = 85f;
                    break;
            }
            if (persist)
            {
                PlayerPrefs.SetInt(QualityKey, profile);
                PlayerPrefs.Save();
            }
            if (currentWorld != null)
            {
                currentWorld.MaxDrawDistance = Mathf.Clamp(startDistance, minDistance, maxDistance);
            }
            slowSamples = 0;
            fastSamples = 0;
            nextChange = Time.unscaledTime + 12f;
        }

        private void Update()
        {
            if (!Loader.HasLoaded) return;
            // The upstream Cell can spend up to 10ms updating map areas in one frame.
            // Scale this CPU budget with the requested FPS, accepting possible extra pop-in.
            var cell = Cell.Instance;
            if (cell != null)
            {
                float frameMs = 1000f / Mathf.Max(30, Application.targetFrameRate);
                ushort areaBudgetMs = (ushort)Mathf.Clamp(frameMs * 0.33f, 3f, 7f);
                if (cell.maxTimeToUpdatePerFrameMs != areaBudgetMs)
                    cell.maxTimeToUpdatePerFrameMs = areaBudgetMs;
            }
            var world = WorldManager.Singleton;
            if (world == null) return;

            if (currentWorld != world)
            {
                currentWorld = world;
                // Apply default mobile distance once for each newly loaded world.
                currentWorld.MaxDrawDistance = Mathf.Min(world.MaxDrawDistance, startDistance);
                nextChange = Time.unscaledTime + 12f;
                slowSamples = fastSamples = 0;
                return;
            }

            if (governor == null || governor.SampledFrameMs <= 0f || Time.unscaledTime < nextChange)
                return;

            float frameBudget = 1000f / Mathf.Max(1, Application.targetFrameRate);
            float currentMs = governor.SampledFrameMs;
            if (currentMs > frameBudget * 1.15f)
            {
                slowSamples++;
                fastSamples = 0;
            }
            else if (currentMs < frameBudget * 0.73f)
            {
                fastSamples++;
                slowSamples = 0;
            }
            else
            {
                fastSamples = slowSamples = 0;
            }

            if (slowSamples >= 2)
            {
                currentWorld.MaxDrawDistance = Mathf.Max(minDistance, currentWorld.MaxDrawDistance - 100f);
                slowSamples = 0;
                nextChange = Time.unscaledTime + 10f;
            }
            else if (fastSamples >= 4)
            {
                currentWorld.MaxDrawDistance = Mathf.Min(maxDistance, currentWorld.MaxDrawDistance + 75f);
                fastSamples = 0;
                nextChange = Time.unscaledTime + 10f;
            }
            else
            {
                nextChange = Time.unscaledTime + 5f;
            }
        }
    }
}
