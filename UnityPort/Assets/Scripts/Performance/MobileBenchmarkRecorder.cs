// SPDX-License-Identifier: MIT
// Original lightweight 60-second frame-time recorder for Android development APKs.
using System;
using System.Globalization;
using System.IO;
using UnityEngine;

namespace SAEngineAndroid.Performance
{
    public sealed class MobileBenchmarkRecorder : MonoBehaviour
    {
        private const float DurationSeconds = 60f;
        private readonly float[] frameTimesMs = new float[18000];
        private int count;
        private float elapsed;
        private bool recording;

        public bool IsRecording { get { return recording; } }
        public float RemainingSeconds { get { return recording ? Mathf.Max(0f, DurationSeconds - elapsed) : 0f; } }
        public string LastResultFilename { get; private set; } = "";

        public void StartRecording()
        {
            if (recording) return;
            count = 0;
            elapsed = 0f;
            recording = true;
            LastResultFilename = "";
        }

        private void Update()
        {
            if (!recording) return;
            float dt = Time.unscaledDeltaTime;
            if (dt > 0f && dt <= 1f)
            {
                elapsed += dt;
                if (count < frameTimesMs.Length)
                    frameTimesMs[count++] = dt * 1000f;
            }
            if (elapsed >= DurationSeconds)
            {
                recording = false;
                Export();
            }
        }

        private void Export()
        {
            if (count == 0) return;
            float[] sorted = new float[count];
            Array.Copy(frameTimesMs, sorted, count);
            Array.Sort(sorted);
            double sum = 0.0;
            for (int i = 0; i < count; i++) sum += sorted[i];

            double meanMs = sum / count;
            float p95 = sorted[Mathf.Clamp((int)Math.Ceiling(count * 0.95) - 1, 0, count - 1)];
            float p99 = sorted[Mathf.Clamp((int)Math.Ceiling(count * 0.99) - 1, 0, count - 1)];
            string filename = "sa_benchmark_" + DateTime.UtcNow.ToString("yyyyMMdd_HHmmss", CultureInfo.InvariantCulture) + ".csv";
            try
            {
                string fullPath = Path.Combine(Application.persistentDataPath, filename);
                using (var writer = new StreamWriter(fullPath, false))
                {
                    writer.WriteLine("metric,value,unit");
                    WriteMetric(writer, "target_fps", Application.targetFrameRate, "fps");
                    WriteMetric(writer, "samples", count, "frames");
                    WriteMetric(writer, "elapsed", elapsed, "s");
                    WriteMetric(writer, "mean_frame_time", meanMs, "ms");
                    WriteMetric(writer, "p95_frame_time", p95, "ms");
                    WriteMetric(writer, "p99_frame_time", p99, "ms");
                    WriteMetric(writer, "mean_fps_equivalent", meanMs > 0 ? 1000.0 / meanMs : 0.0, "fps");
                    writer.WriteLine();
                    writer.WriteLine("sample_index,frame_time_ms");
                    for (int i = 0; i < count; i++)
                    {
                        writer.Write(i + 1);
                        writer.Write(',');
                        writer.WriteLine(frameTimesMs[i].ToString("F3", CultureInfo.InvariantCulture));
                    }
                }
                LastResultFilename = filename;
                Debug.Log("SA Engine Android frame benchmark saved: " + fullPath);
            }
            catch (Exception ex)
            {
                LastResultFilename = "ERROR";
                Debug.Log("Could not save Android benchmark: " + ex.Message);
            }
        }

        private static void WriteMetric(StreamWriter writer, string name, double value, string unit)
        {
            writer.WriteLine(name + "," + value.ToString("F3", CultureInfo.InvariantCulture) + "," + unit);
        }
    }
}
