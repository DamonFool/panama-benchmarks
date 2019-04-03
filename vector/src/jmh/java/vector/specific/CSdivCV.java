/*****************************************************************************
 * Copyright (c) 2019, Lev Serebryakov <lev@serebryakov.spb.ru>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ****************************************************************************/

package vector.specific;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.Vector;
import org.openjdk.jmh.annotations.*;

import java.util.Random;

/** @noinspection PointlessArithmeticExpression, CStyleArrayDeclaration */
@Fork(2)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Threads(1)
@State(Scope.Thread)
public class CSdivCV {
    private final static int SEED = 42; // Carefully selected, pucked by hands random number

    private final static FloatVector.FloatSpecies PFS = FloatVector.preferredSpecies();
    private final static int EPV = PFS.length();
    private final static int EPV2 = EPV / 2;
    private final static FloatVector.FloatSpecies FS64 = FloatVector.species(Vector.Shape.S_64_BIT);

    private final static Vector.Mask<Float> MASK_C_IM;

    private final static Vector.Shuffle<Float> SHUFFLE_CS_TO_CV_SPREAD = FloatVector.shuffle(PFS, i -> i % 2);
    private final static Vector.Shuffle<Float> SHUFFLE_CV_SWAP_RE_IM = FloatVector.shuffle(PFS, i -> (i % 2 == 0) ? i + 1 : i - 1);
    private final static Vector.Shuffle<Float> SHUFFLE_CV_SPREAD_RE = FloatVector.shuffle(PFS, i -> i - i % 2);
    private final static Vector.Shuffle<Float> SHUFFLE_CV_SPREAD_IM = FloatVector.shuffle(PFS, i -> i - i % 2 + 1);

    private final static int[] LOAD_CS_TO_CV_SPREAD = SHUFFLE_CS_TO_CV_SPREAD.toArray();
    private final static int[] LOAD_CV_TO_CV_SPREAD_RE = SHUFFLE_CV_SPREAD_RE.toArray();
    private final static int[] LOAD_CV_TO_CV_SPREAD_IM = SHUFFLE_CV_SPREAD_IM.toArray();

    static {
        boolean[] alter = new boolean[EPV + 1];
        alter[0] = true;
        for (int i = 1; i < alter.length; i++)
            alter[i] = !alter[i-1];
        MASK_C_IM = FloatVector.maskFromArray(PFS, alter, 1);
    }

    private float z[];
    private float x[];
    private float y[];
    private int count = 16;

    @Setup
    public void Setup() {
        Random r = new Random(SEED);

        x = new float[2];
        y = new float[65536 * 2];
        z = new float[65536 * 2];

        for (int i = 0; i < y.length; i++)
            y[i] = r.nextFloat() * 2.0f - 1.0f;
        x[0] = r.nextFloat() * 2.0f - 1.0f;
        x[1] = r.nextFloat() * 2.0f - 1.0f;
    }

    @Benchmark
    public void loadepv_load1() {
        cs_div_cv_long_load_one_load(z, 0, x, y, 0, count);
    }

    @Benchmark
    public void loadepv_load2() {
        cs_div_cv_long_load_two_loads(z, 0, x, y, 0, count);
    }

    @Benchmark
    public void load64_load1() {
        cs_div_cv_short_load_one_load(z, 0, x, y, 0, count);
    }

    @Benchmark
    public void load64_load2() {
        cs_div_cv_short_load_two_loads(z, 0, x, y, 0, count);
    }

    public static void cs_div_cv_long_load_one_load(float z[], int zOffset, float x[], float y[], int yOffset, int count) {
        FloatVector vx = null;
        if (count >= EPV2)
            vx = FloatVector.fromArray(PFS, x, 0, LOAD_CS_TO_CV_SPREAD, 0);

        zOffset <<= 1;
        yOffset <<= 1;

        while (count >= EPV2) {
            // vy is [(y[0].re, y[0].im), (y[1].re, y[1].im), ...]
            final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
            // vyre is [(y[0].re, y[0].re), (y[1].re, y[1].re), ...]
            final FloatVector vyre = vy.rearrange(SHUFFLE_CV_SPREAD_RE);
            // vyim is [(y[0].im, y[0].im), (y[1].im, y[1].im), ...]
            final FloatVector vyim = vy.rearrange(SHUFFLE_CV_SPREAD_IM);

            // vmulxre [(x.re * y[0].re, x.im * y[0].re), (x.re * y[1].re, x.im * y[1].re), ...]
            final FloatVector vmulxre = vx.mul(vyre);
            // vmulxim [(x.re * y[0].im, x.im * y[0].im), (x.re * y[1].im, x.im * y[1].im), ...]
            final FloatVector vmulxim = vx.mul(vyim);
            // vmulximswap is [(x.im * y[0].im, x.re * y[0].im), (x.im * y[1].im, x.re * y[1].im), ...]
            final FloatVector vmulximswap = vmulxim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

            // Get abs to divide
            final FloatVector vysq = vyre.mul(vyre).add(vyim.mul(vyim));

            // vrre is [(x.re * y[0].re + x.im * y[0].im, ?), (x.re * y[1].re + x.im * y[1].im, ?), ...]
            final FloatVector vrre = vmulxre.add(vmulximswap);
            // vrim it is [(?, x.im * y[0].re - x.re * y[0].im), (?, x.im * y[1].re - x.re * y[1].im), ...]
            final FloatVector vrim = vmulxre.sub(vmulximswap);

            // Divide, Blend together & save
            vrre.blend(vrim, MASK_C_IM).div(vysq).intoArray(z, zOffset);

            yOffset += EPV;
            zOffset += EPV;
            count -= EPV2;
        }

        float sq;
        while (count-- > 0) {
            sq = y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1];
            z[zOffset + 0] = (x[0] * y[yOffset + 0] + x[1] * y[yOffset + 1]) / sq;
            z[zOffset + 1] = (x[1] * y[yOffset + 0] - x[0] * y[yOffset + 1]) / sq;
            yOffset += 2;
            zOffset += 2;
        }
    }

    public static void cs_div_cv_short_load_one_load(float z[], int zOffset, float x[], float y[], int yOffset, int count) {
        FloatVector vx = null;
        if (count >= EPV2)
            vx = FloatVector.fromArray(FS64, x, 0).reshape(PFS).rearrange(SHUFFLE_CS_TO_CV_SPREAD);

        zOffset <<= 1;
        yOffset <<= 1;

        while (count >= EPV2) {
            // vy is [(y[0].re, y[0].im), (y[1].re, y[1].im), ...]
            final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
            // vyre is [(y[0].re, y[0].re), (y[1].re, y[1].re), ...]
            final FloatVector vyre = vy.rearrange(SHUFFLE_CV_SPREAD_RE);
            // vyim is [(y[0].im, y[0].im), (y[1].im, y[1].im), ...]
            final FloatVector vyim = vy.rearrange(SHUFFLE_CV_SPREAD_IM);

            // vmulxre [(x.re * y[0].re, x.im * y[0].re), (x.re * y[1].re, x.im * y[1].re), ...]
            final FloatVector vmulxre = vx.mul(vyre);
            // vmulxim [(x.re * y[0].im, x.im * y[0].im), (x.re * y[1].im, x.im * y[1].im), ...]
            final FloatVector vmulxim = vx.mul(vyim);
            // vmulximswap is [(x.im * y[0].im, x.re * y[0].im), (x.im * y[1].im, x.re * y[1].im), ...]
            final FloatVector vmulximswap = vmulxim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

            // Get abs to divide
            final FloatVector vysq = vyre.mul(vyre).add(vyim.mul(vyim));

            // vrre is [(x.re * y[0].re + x.im * y[0].im, ?), (x.re * y[1].re + x.im * y[1].im, ?), ...]
            final FloatVector vrre = vmulxre.add(vmulximswap);
            // vrim it is [(?, x.im * y[0].re - x.re * y[0].im), (?, x.im * y[1].re - x.re * y[1].im), ...]
            final FloatVector vrim = vmulxre.sub(vmulximswap);

            // Divide, Blend together & save
            vrre.blend(vrim, MASK_C_IM).div(vysq).intoArray(z, zOffset);

            yOffset += EPV;
            zOffset += EPV;
            count -= EPV2;
        }

        float sq;
        while (count-- > 0) {
            sq = y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1];
            z[zOffset + 0] = (x[0] * y[yOffset + 0] + x[1] * y[yOffset + 1]) / sq;
            z[zOffset + 1] = (x[1] * y[yOffset + 0] - x[0] * y[yOffset + 1]) / sq;
            yOffset += 2;
            zOffset += 2;
        }
    }

    public static void cs_div_cv_long_load_two_loads(float z[], int zOffset, float x[], float y[], int yOffset, int count) {
        FloatVector vx = null;
        if (count >= EPV2)
            vx = FloatVector.fromArray(PFS, x, 0, LOAD_CS_TO_CV_SPREAD, 0);

        zOffset <<= 1;
        yOffset <<= 1;

        while (count >= EPV2) {
            // vyre is [(y[0].re, y[0].re), (y[1].re, y[1].re), ...]
            final FloatVector vyre = FloatVector.fromArray(PFS, y, yOffset, LOAD_CV_TO_CV_SPREAD_RE, 0);
            // vyim is [(y[0].im, y[0].im), (y[1].im, y[1].im), ...]
            final FloatVector vyim = FloatVector.fromArray(PFS, y, yOffset, LOAD_CV_TO_CV_SPREAD_IM, 0);

            // vmulxre [(x.re * y[0].re, x.im * y[0].re), (x.re * y[1].re, x.im * y[1].re), ...]
            final FloatVector vmulxre = vx.mul(vyre);
            // vmulxim [(x.re * y[0].im, x.im * y[0].im), (x.re * y[1].im, x.im * y[1].im), ...]
            final FloatVector vmulxim = vx.mul(vyim);
            // vmulximswap is [(x.im * y[0].im, x.re * y[0].im), (x.im * y[1].im, x.re * y[1].im), ...]
            final FloatVector vmulximswap = vmulxim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

            // Get abs to divide
            final FloatVector vysq = vyre.mul(vyre).add(vyim.mul(vyim));

            // vrre is [(x.re * y[0].re + x.im * y[0].im, ?), (x.re * y[1].re + x.im * y[1].im, ?), ...]
            final FloatVector vrre = vmulxre.add(vmulximswap);
            // vrim it is [(?, x.im * y[0].re - x.re * y[0].im), (?, x.im * y[1].re - x.re * y[1].im), ...]
            final FloatVector vrim = vmulxre.sub(vmulximswap);

            // Divide, Blend together & save
            vrre.blend(vrim, MASK_C_IM).div(vysq).intoArray(z, zOffset);

            yOffset += EPV;
            zOffset += EPV;
            count -= EPV2;
        }

        float sq;
        while (count-- > 0) {
            sq = y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1];
            z[zOffset + 0] = (x[0] * y[yOffset + 0] + x[1] * y[yOffset + 1]) / sq;
            z[zOffset + 1] = (x[1] * y[yOffset + 0] - x[0] * y[yOffset + 1]) / sq;
            yOffset += 2;
            zOffset += 2;
        }
    }

    public static void cs_div_cv_short_load_two_loads(float z[], int zOffset, float x[], float y[], int yOffset, int count) {
        FloatVector vx = null;
        if (count >= EPV2)
            vx = FloatVector.fromArray(FS64, x, 0).reshape(PFS).rearrange(SHUFFLE_CS_TO_CV_SPREAD);

        zOffset <<= 1;
        yOffset <<= 1;

        while (count >= EPV2) {
            // vyre is [(y[0].re, y[0].re), (y[1].re, y[1].re), ...]
            final FloatVector vyre = FloatVector.fromArray(PFS, y, yOffset, LOAD_CV_TO_CV_SPREAD_RE, 0);
            // vyim is [(y[0].im, y[0].im), (y[1].im, y[1].im), ...]
            final FloatVector vyim = FloatVector.fromArray(PFS, y, yOffset, LOAD_CV_TO_CV_SPREAD_IM, 0);

            // vmulxre [(x.re * y[0].re, x.im * y[0].re), (x.re * y[1].re, x.im * y[1].re), ...]
            final FloatVector vmulxre = vx.mul(vyre);
            // vmulxim [(x.re * y[0].im, x.im * y[0].im), (x.re * y[1].im, x.im * y[1].im), ...]
            final FloatVector vmulxim = vx.mul(vyim);
            // vmulximswap is [(x.im * y[0].im, x.re * y[0].im), (x.im * y[1].im, x.re * y[1].im), ...]
            final FloatVector vmulximswap = vmulxim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

            // Get abs to divide
            final FloatVector vysq = vyre.mul(vyre).add(vyim.mul(vyim));

            // vrre is [(x.re * y[0].re + x.im * y[0].im, ?), (x.re * y[1].re + x.im * y[1].im, ?), ...]
            final FloatVector vrre = vmulxre.add(vmulximswap);
            // vrim it is [(?, x.im * y[0].re - x.re * y[0].im), (?, x.im * y[1].re - x.re * y[1].im), ...]
            final FloatVector vrim = vmulxre.sub(vmulximswap);

            // Divide, Blend together & save
            vrre.blend(vrim, MASK_C_IM).div(vysq).intoArray(z, zOffset);

            yOffset += EPV;
            zOffset += EPV;
            count -= EPV2;
        }

        float sq;
        while (count-- > 0) {
            sq = y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1];
            z[zOffset + 0] = (x[0] * y[yOffset + 0] + x[1] * y[yOffset + 1]) / sq;
            z[zOffset + 1] = (x[1] * y[yOffset + 0] - x[0] * y[yOffset + 1]) / sq;
            yOffset += 2;
            zOffset += 2;
        }
    }
}
