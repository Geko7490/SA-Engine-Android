// SPDX-License-Identifier: MIT
using UnityEngine;

namespace SAEngineAndroid.Performance
{
    /// <summary>
    /// Touch-friendly development overlay. Hidden in non-development APKs.
    /// Provides in-game FPS targets and visual presets without a PC.
    /// </summary>
    public sealed class MobilePerformancePanel : MonoBehaviour
    {
        private MobilePerformanceGovernor governor;
        private MobileWorldTuner worldTuner;
        private bool expanded;
        private float nextTextTime;
        private string frameText = "FPS: --";

        private void Awake()
        {
            governor = GetComponent<MobilePerformanceGovernor>();
            worldTuner = GetComponent<MobileWorldTuner>();
        }

        private void Update()
        {
            if (Time.unscaledTime < nextTextTime || governor == null) return;
            frameText = string.Format("FPS {0:F1} | {1:F1} ms | Draw {2:F0}m",
                governor.SampledFps, governor.SampledFrameMs,
                worldTuner != null ? worldTuner.CurrentDrawDistance : 0f);
            nextTextTime = Time.unscaledTime + 0.5f;
        }

        private void OnGUI()
        {
            if (!Debug.isDebugBuild || governor == null || worldTuner == null) return;
            float scale = Screen.dpi > 0 ? Mathf.Clamp(Screen.dpi / 160f, 1f, 3f) : 2f;
            Matrix4x4 previous = GUI.matrix;
            GUI.matrix = Matrix4x4.Scale(new Vector3(scale, scale, 1f));
            try
            {
                float right = Screen.safeArea.xMax / scale;
                float top = Screen.safeArea.yMin / scale + 6f;
                if (!expanded)
                {
                    if (GUI.Button(new Rect(Mathf.Max(0f, right - 78f), top, 74f, 38f), "FPS"))
                        expanded = true;
                    return;
                }

                Rect bounds = new Rect(Mathf.Max(0f, right - 290f), top, 285f, 212f);
                GUILayout.BeginArea(bounds, GUI.skin.box);
                GUILayout.BeginHorizontal();
                GUILayout.Label("PERFORMANCE");
                if (GUILayout.Button("X", GUILayout.Width(38f), GUILayout.Height(36f)))
                    expanded = false;
                GUILayout.EndHorizontal();
                GUILayout.Label(frameText);
                GUILayout.BeginHorizontal();
                foreach (int fps in new[] { 30, 60, 90, 120 })
                {
                    if (GUILayout.Button(fps.ToString(), GUILayout.Height(36f)))
                        governor.SetTargetFps(fps);
                }
                GUILayout.EndHorizontal();
                GUILayout.BeginHorizontal();
                if (GUILayout.Button("LOW", GUILayout.Height(38f))) worldTuner.ApplyProfile(0);
                if (GUILayout.Button("MED", GUILayout.Height(38f))) worldTuner.ApplyProfile(1);
                if (GUILayout.Button("HIGH", GUILayout.Height(38f))) worldTuner.ApplyProfile(2);
                GUILayout.EndHorizontal();
                GUILayout.Label("Display refresh and device limits still apply.");
                GUILayout.EndArea();
            }
            finally
            {
                GUI.matrix = previous;
            }
        }
    }
}
