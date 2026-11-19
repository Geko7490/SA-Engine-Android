// SPDX-License-Identifier: MIT
// Original fallback decoder for Android GPUs without S3TC/DXT support.
// No Unity API calls: suitable for testing as a pure data transform.
using System;

namespace SanAndreasUnity.Importing.Conversion
{
    internal static class MobileDxtDecoder
    {
        public static byte[] DecodeDxt1(byte[] input, int width, int height)
        {
            return Decode(input, width, height, false);
        }

        public static byte[] DecodeDxt3(byte[] input, int width, int height)
        {
            return Decode(input, width, height, true);
        }

        private static byte[] Decode(byte[] input, int width, int height, bool hasExplicitAlpha)
        {
            if (input == null) throw new ArgumentNullException("input");
            if (width <= 0 || height <= 0) throw new ArgumentOutOfRangeException("width");
            int blocksWide = checked((width + 3) / 4);
            int blocksHigh = checked((height + 3) / 4);
            int blockSize = hasExplicitAlpha ? 16 : 8;
            int required = checked(checked(blocksWide * blocksHigh) * blockSize);
            if (input.Length < required)
                throw new ArgumentException("Truncated DXT texture data.", "input");
            byte[] result = new byte[checked(checked(width * height) * 4)];
            byte[] palette = new byte[16];

            for (int by = 0; by < blocksHigh; by++)
            for (int bx = 0; bx < blocksWide; bx++)
            {
                int blockStart = (by * blocksWide + bx) * blockSize;
                int colorStart = blockStart + (hasExplicitAlpha ? 8 : 0);
                int c0 = input[colorStart] | (input[colorStart + 1] << 8);
                int c1 = input[colorStart + 2] | (input[colorStart + 3] << 8);
                Write565(palette, 0, c0);
                Write565(palette, 4, c1);
                bool fourColor = hasExplicitAlpha || c0 > c1;
                for (int channel = 0; channel < 3; channel++)
                {
                    int a = palette[channel];
                    int b = palette[4 + channel];
                    palette[8 + channel] = (byte)(fourColor ? (2 * a + b) / 3 : (a + b) / 2);
                    palette[12 + channel] = (byte)(fourColor ? (a + 2 * b) / 3 : 0);
                }
                palette[11] = 255;
                palette[15] = (byte)(fourColor ? 255 : 0);

                uint indexes = (uint)(input[colorStart + 4] |
                    (input[colorStart + 5] << 8) |
                    (input[colorStart + 6] << 16) |
                    (input[colorStart + 7] << 24));
                for (int pixel = 0; pixel < 16; pixel++)
                {
                    int localX = pixel & 3;
                    int localY = pixel >> 2;
                    int x = bx * 4 + localX;
                    int y = by * 4 + localY;
                    if (x >= width || y >= height) continue;
                    int colorIndex = (int)((indexes >> (pixel * 2)) & 3) * 4;
                    int output = (y * width + x) * 4;
                    result[output] = palette[colorIndex];
                    result[output + 1] = palette[colorIndex + 1];
                    result[output + 2] = palette[colorIndex + 2];
                    if (hasExplicitAlpha)
                    {
                        int alphaByte = input[blockStart + (pixel >> 1)];
                        int alphaNibble = (alphaByte >> ((pixel & 1) * 4)) & 15;
                        result[output + 3] = (byte)(alphaNibble * 17);
                    }
                    else
                    {
                        result[output + 3] = palette[colorIndex + 3];
                    }
                }
            }
            return result;
        }

        private static void Write565(byte[] colors, int offset, int packed)
        {
            int r = (packed >> 11) & 31;
            int g = (packed >> 5) & 63;
            int b = packed & 31;
            colors[offset] = (byte)((r << 3) | (r >> 2));
            colors[offset + 1] = (byte)((g << 2) | (g >> 4));
            colors[offset + 2] = (byte)((b << 3) | (b >> 2));
            colors[offset + 3] = 255;
        }
    }
}
