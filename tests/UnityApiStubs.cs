// SPDX-License-Identifier: MIT
// Minimal stub API to syntax-check overlay C# on .NET without Unity licensing.
// NOT an emulator; a successful build does not guarantee Unity runtime compatibility.
using System;
using System.Collections.Generic;

namespace UnityEngine
{
    [AttributeUsage(AttributeTargets.Field)] public sealed class SerializeField : Attribute { }
    [AttributeUsage(AttributeTargets.Field)] public sealed class HeaderAttribute : Attribute { public HeaderAttribute(string label) { } }
    public enum RuntimeInitializeLoadType { BeforeSceneLoad, AfterSceneLoad }
    [AttributeUsage(AttributeTargets.Method)]
    public sealed class RuntimeInitializeOnLoadMethodAttribute : Attribute
    { public RuntimeInitializeOnLoadMethodAttribute(RuntimeInitializeLoadType mode) { } }

    public class Object
    {
        public static T FindObjectOfType<T>() where T : class { return null; }
        public static void DontDestroyOnLoad(Object value) { }
    }

    public class GameObject : Object
    {
        public GameObject(string name) { }
        public T AddComponent<T>() where T : new() { return new T(); }
    }

    public class MonoBehaviour : Object
    {
        public T GetComponent<T>() where T : class { return null; }
    }

    public static class Application { public static int targetFrameRate = 60; public static string dataPath = "/tmp/SanAndreasUnity/Assets"; public static string persistentDataPath = "/tmp/sa-engine-data"; }
    public static class Time { public static float unscaledTime; public static float unscaledDeltaTime; }
    public static class Debug { public static bool isDebugBuild = true; public static void Log(object message) { } }

    public static class QualitySettings
    {
        public static int vSyncCount, masterTextureLimit, antiAliasing;
        public static float shadowDistance = 85f;
    }
    public static class PlayerPrefs
    {
        static readonly Dictionary<string, int> values = new Dictionary<string, int>();
        public static int GetInt(string key, int fallback) { return values.TryGetValue(key, out int value) ? value : fallback; }
        public static void SetInt(string key, int value) { values[key] = value; }
        public static void Save() { }
    }
    public static class Mathf
    {
        public static float Max(float a, float b) { return Math.Max(a,b); }
        public static int Max(int a, int b) { return Math.Max(a,b); }
        public static float Min(float a, float b) { return Math.Min(a,b); }
        public static float Clamp(float value, float min, float max) { return Math.Max(min, Math.Min(max,value)); }
        public static int Clamp(int value, int min, int max) { return Math.Max(min, Math.Min(max,value)); }
    }
    public struct Rect
    {
        public float xMin, yMin, width, height;
        public Rect(float x, float y, float w, float h) { xMin=x; yMin=y; width=w; height=h; }
        public float xMax { get { return xMin + width; } }
    }
    public struct Vector3
    {
        public float x, y, z;
        public Vector3(float x, float y, float z) { this.x=x; this.y=y; this.z=z; }
    }
    public struct Matrix4x4
    {
        public static Matrix4x4 Scale(Vector3 scale) { return new Matrix4x4(); }
    }
    public static class Screen
    {
        public static float dpi = 320f;
        public static Rect safeArea = new Rect(0, 0, 1920, 1080);
    }
    public class GUIStyle { }
    public class GUISkin { public GUIStyle box = new GUIStyle(); }
    public static class GUI
    {
        public static Matrix4x4 matrix;
        public static GUISkin skin = new GUISkin();
        public static bool Button(Rect rect, string label) { return false; }
    }
    public class GUILayoutOption { }
    public static class GUILayout
    {
        public static void BeginArea(Rect rect, GUIStyle style) { }
        public static void EndArea() { }
        public static void BeginHorizontal() { }
        public static void EndHorizontal() { }
        public static void Label(string label) { }
        public static bool Button(string label, params GUILayoutOption[] options) { return false; }
        public static GUILayoutOption Width(float width) { return new GUILayoutOption(); }
        public static GUILayoutOption Height(float height) { return new GUILayoutOption(); }
    }
}
namespace SanAndreasUnity.Behaviours
{
    public static class Loader { public static bool HasLoaded = true; }
}
namespace SanAndreasUnity.Behaviours.World
{
    public sealed class Cell
    {
        public static Cell Instance { get; } = new Cell();
        public ushort maxTimeToUpdatePerFrameMs = 10;
    }
    public sealed class WorldManager
    {
        public static WorldManager Singleton { get; } = new WorldManager();
        public float MaxDrawDistance { get; set; } = 1000f;
    }
}


namespace UnityEditor
{
    public enum BuildTarget { Android }
    public enum BuildTargetGroup { Android }
    public enum AndroidBuildSystem { Gradle }
    public enum ScriptingImplementation { IL2CPP }
    public enum AndroidArchitecture { ARM64 }
    public enum AndroidSdkVersions { AndroidApiLevel26, AndroidApiLevelAuto }
    [Flags] public enum BuildOptions { Development = 1, AllowDebugging = 2 }
    public sealed class EditorBuildSettingsScene { public bool enabled; public string path = ""; }
    public static class EditorBuildSettings
    {
        public static EditorBuildSettingsScene[] scenes = Array.Empty<EditorBuildSettingsScene>();
    }
    public static class EditorUserBuildSettings
    {
        public static bool buildAppBundle;
        public static AndroidBuildSystem androidBuildSystem;
    }
    public static class PlayerSettings
    {
        public static string productName = "";
        public static void SetScriptingBackend(BuildTargetGroup target, ScriptingImplementation value) { }
        public static void SetApplicationIdentifier(BuildTargetGroup target, string id) { }
        public static class Android
        {
            public static AndroidArchitecture targetArchitectures;
            public static AndroidSdkVersions minSdkVersion;
            public static AndroidSdkVersions targetSdkVersion;
        }
    }
    public struct BuildPlayerOptions
    {
        public string[] scenes;
        public string locationPathName;
        public BuildTarget target;
        public BuildOptions options;
    }
    public static class BuildPipeline
    {
        public static Build.Reporting.BuildReport BuildPlayer(BuildPlayerOptions options)
        {
            return new Build.Reporting.BuildReport();
        }
    }
}
namespace UnityEditor.Build.Reporting
{
    public enum BuildResult { Succeeded, Failed }
    public sealed class BuildSummary
    {
        public BuildResult result = BuildResult.Succeeded;
        public long totalSize;
    }
    public sealed class BuildReport
    {
        public BuildSummary summary = new BuildSummary();
    }
}
