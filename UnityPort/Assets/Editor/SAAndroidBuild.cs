// SPDX-License-Identifier: MIT
// Build script for a licensed local SanAndreasUnity checkout with overlay applied.
#if UNITY_EDITOR
using System;
using System.IO;
using UnityEditor;
using UnityEditor.Build.Reporting;
using UnityEngine;

public static class SAAndroidBuild
{
    // CLI: Unity -batchmode -quit -projectPath ... -executeMethod SAAndroidBuild.BuildArm64
    public static void BuildArm64()
    {
        var scenes = EditorBuildSettings.scenes;
        var enabledScenes = Array.FindAll(scenes, s => s.enabled);
        string[] paths;
        if (enabledScenes.Length == 0)
        {
            const string startup = "Assets/Scenes/Startup.unity";
            if (!File.Exists(startup))
                throw new InvalidOperationException("No enabled build scenes and Startup.unity is missing.");
            paths = new[] { startup };
        }
        else
        {
            paths = Array.ConvertAll(enabledScenes, s => s.path);
        }
        string apk = Environment.GetEnvironmentVariable("SA_ANDROID_APK_PATH");
        if (string.IsNullOrEmpty(apk)) apk = "Builds/SAEngineAndroid-dev.apk";
        string dir = Path.GetDirectoryName(apk);
        if (!string.IsNullOrEmpty(dir)) Directory.CreateDirectory(dir);

        EditorUserBuildSettings.buildAppBundle = false;
        EditorUserBuildSettings.androidBuildSystem = AndroidBuildSystem.Gradle;
        PlayerSettings.SetScriptingBackend(BuildTargetGroup.Android, ScriptingImplementation.IL2CPP);
        PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARM64;
        PlayerSettings.Android.minSdkVersion = AndroidSdkVersions.AndroidApiLevel26;
        PlayerSettings.Android.targetSdkVersion = AndroidSdkVersions.AndroidApiLevelAuto;
        // Preserve project-specific rendering settings; no assumption that Vulkan works yet.
        var options = new BuildPlayerOptions
        {
            scenes = paths,
            locationPathName = apk,
            target = BuildTarget.Android,
            options = BuildOptions.Development | BuildOptions.AllowDebugging
        };
        var report = BuildPipeline.BuildPlayer(options);
        if (report.summary.result != BuildResult.Succeeded)
            throw new Exception("Android build failed: " + report.summary.result);
        Debug.Log("Android APK built at: " + apk + " size=" + report.summary.totalSize);
    }
}
#endif
