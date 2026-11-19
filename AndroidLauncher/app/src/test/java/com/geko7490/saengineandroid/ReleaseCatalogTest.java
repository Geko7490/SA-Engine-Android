package com.geko7490.saengineandroid;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

public class ReleaseCatalogTest {
    private static final String API = "https://api.github.com/repos/Geko7490/SA-Engine-Android/releases/assets/";
    private static String digest() {
        StringBuilder result = new StringBuilder("sha256:");
        for (int i = 0; i < 64; i++) result.append('a');
        return result.toString();
    }

    private static JSONObject asset(long id, String name, String digest) throws Exception {
        return new JSONObject()
                .put("id", id)
                .put("url", API + id)
                .put("name", name)
                .put("size", 1024)
                .put("digest", digest);
    }
    private static JSONObject release(String tag, String body, JSONObject... assets) throws Exception {
        JSONArray list = new JSONArray();
        for (JSONObject a : assets) list.put(a);
        return new JSONObject()
                .put("tag_name", tag).put("body", body).put("assets", list);
    }

    @Test public void engineReleaseRecognizedWithCompatibilityAndHash() throws Exception {
        JSONArray data = new JSONArray().put(release("engine-dev",
                "minAndroidApi=26\nGame test",
                asset(12, "SAEngineGame-0.6-dev.apk", digest())));
        List<ReleaseCatalog.PackageEntry> list = ReleaseCatalog.parseReleases(data);
        assertEquals(1, list.size());
        assertTrue(list.get(0).isEngine());
        assertEquals(26, list.get(0).minAndroid);
        assertEquals(64, list.get(0).digest.length());
        assertEquals("engine-dev", list.get(0).tag);
    }

    @Test public void launcherPreviewNeverMislabelledAsPlayReady() throws Exception {
        JSONArray data = new JSONArray().put(release("launcher-dev", "",
                asset(14, "app-debug.apk", digest())));
        List<ReleaseCatalog.PackageEntry> list = ReleaseCatalog.parseReleases(data);
        assertEquals(1, list.size());
        assertFalse(list.get(0).isEngine());
    }

    @Test public void malformedDigestAndForgedUrlAreRejected() throws Exception {
        JSONObject forged = asset(16, "SAEngineGame-0.6.apk", digest())
                .put("url", "https://fake.example.com/file.apk");
        JSONArray data = new JSONArray().put(release("engine-dev", "",
                asset(13, "SAEngineGame-0.6.apk", "sha256:bad"),
                forged,
                asset(17, "unknown.apk", digest())));
        assertEquals(0, ReleaseCatalog.parseReleases(data).size());
    }

    @Test public void draftReleasesNotListed() throws Exception {
        JSONObject draft = release("engine-dev", "", asset(10, "SAEngineGame-0.6.apk", digest()))
                .put("draft", true);
        assertTrue(ReleaseCatalog.parseReleases(new JSONArray().put(draft)).isEmpty());
    }
}
