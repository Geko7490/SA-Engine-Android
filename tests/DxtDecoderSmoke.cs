// SPDX-License-Identifier: MIT
// Standalone .NET smoke tests for the exact C# decoder also included in Unity.
using System;
using SanAndreasUnity.Importing.Conversion;

static class DecoderSmoke
{
    static int assertions;

    static void Check(bool ok, string message)
    {
        assertions++;
        if (!ok) throw new Exception("DXT decoder regression: " + message);
    }

    static byte[] Block565(ushort c0, ushort c1, uint indexes)
    {
        byte[] block = new byte[8];
        block[0] = (byte)c0; block[1] = (byte)(c0 >> 8);
        block[2] = (byte)c1; block[3] = (byte)(c1 >> 8);
        for (int i = 0; i < 4; i++) block[4 + i] = (byte)(indexes >> (8 * i));
        return block;
    }

    static void CheckPixel(byte[] data, int pixelIndex, byte r, byte g, byte b, byte a)
    {
        int offset = pixelIndex * 4;
        Check(data[offset] == r && data[offset + 1] == g &&
              data[offset + 2] == b && data[offset + 3] == a,
              "pixel " + pixelIndex + " unexpected RGBA");
    }

    public static int Main()
    {
        try
        {
            // RGB565 red endpoint; 4x4 block selects palette 0 on every pixel.
            var red = MobileDxtDecoder.DecodeDxt1(Block565(0xf800, 0x07e0, 0), 4, 4);
            Check(red.Length == 64, "DXT1 dimensions");
            for (int i = 0; i < 16; i++) CheckPixel(red, i, 255, 0, 0, 255);

            // DXT1 three-colour mode has alpha=0 for index 3.
            var transparent = MobileDxtDecoder.DecodeDxt1(Block565(0, 0xffff, 0xffffffff), 4, 4);
            for (int i = 0; i < 16; i++) CheckPixel(transparent, i, 0, 0, 0, 0);

            // Crop a compressed 4x4 block into a 2x2 texture.
            var cropped = MobileDxtDecoder.DecodeDxt1(Block565(0xf800, 0x07e0, 0), 2, 2);
            Check(cropped.Length == 16, "DXT1 cropping");
            for (int i = 0; i < 4; i++) CheckPixel(cropped, i, 255, 0, 0, 255);

            // DXT3: alpha0=0 and alpha1=15, repeated; opaque red colour block.
            var dxt3 = new byte[16];
            for (int i = 0; i < 8; i++) dxt3[i] = 0xf0;
            Array.Copy(Block565(0xf800, 0x07e0, 0), 0, dxt3, 8, 8);
            var alpha = MobileDxtDecoder.DecodeDxt3(dxt3, 4, 4);
            for (int i = 0; i < 16; i++)
                CheckPixel(alpha, i, 255, 0, 0, (byte)(i % 2 == 0 ? 0 : 255));

            // Two adjacent blocks: independent RGB565 endpoints, correct row stride.
            var dual = new byte[16];
            Array.Copy(Block565(0xf800, 0x07e0, 0), 0, dual, 0, 8);
            Array.Copy(Block565(0x001f, 0x07e0, 0), 0, dual, 8, 8);
            var two = MobileDxtDecoder.DecodeDxt1(dual, 8, 4);
            for (int y = 0; y < 4; y++)
            {
                CheckPixel(two, y * 8, 255, 0, 0, 255);
                CheckPixel(two, y * 8 + 4, 0, 0, 255, 255);
            }

            bool rejected = false;
            try { MobileDxtDecoder.DecodeDxt1(new byte[7], 4, 4); }
            catch (ArgumentException) { rejected = true; }
            Check(rejected, "truncated block must be rejected");

            rejected = false;
            try { MobileDxtDecoder.DecodeDxt3(new byte[16], 0, 4); }
            catch (ArgumentOutOfRangeException) { rejected = true; }
            Check(rejected, "invalid dimensions must be rejected");

            Console.WriteLine("PASS: " + assertions + " DXT pixel and bounds assertions");
            return 0;
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine(ex);
            return 1;
        }
    }
}
