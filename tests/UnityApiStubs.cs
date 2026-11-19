// SPDX-License-Identifier: MIT
// Minimal stub API to syntax-check overlay C# on .NET without Unity licensing.
// NOT an emulator; a successful build does not guarantee Unity runtime compatibility.
using System;
using System.Collections.Generic;

namespace UnityEngine
{
    [AttributeUsage(AttributeTargets.Field)] public sealed class SerializeField : Attribute { }
    [AttributeUsage(AttributeTargets.Field)] public sealed class HeaderAttribute : Attribute { public HeaderAttribute(string label) { } }
    public enum RuntimeInitializeLoadType { AfterSceneLoad }
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

    public static class Application { public static int targetFrameRate = 60; }
    public static class Time { public static float unscaledTime; public static float unscaledDeltaTime; }
    public static class Debug { public static bool isDebugBuild = true; }

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
    public sealed class WorldManager
    {
        public static WorldManager Singleton { get; } = new WorldManager();
        public float MaxDrawDistance { get; set; } = 1000f;
    }
}
