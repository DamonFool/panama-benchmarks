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

package vector;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.Vector;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@Fork(2)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Threads(1)
@State(org.openjdk.jmh.annotations.Scope.Thread)
public class LoadREIM {
    private final static FloatVector.FloatSpecies PFS;
    private final static int[] LOAD_CV_TO_CV_SPREAD_RE;
    private final static int[] LOAD_CV_TO_CV_SPREAD_IM;
    private static final Vector.Shuffle<Float> SHUFFLE_CV_SPREAD_RE;
    private static final Vector.Shuffle<Float> SHUFFLE_CV_SPREAD_IM;

    static {
        PFS = FloatVector.preferredSpecies();
        LOAD_CV_TO_CV_SPREAD_RE = new int[PFS.length()];
        LOAD_CV_TO_CV_SPREAD_IM = new int[PFS.length()];

        for (int i = 0; i < PFS.length(); i++) {
            // Complex vector to complex vector of duplicated real parts:
            // [(re1, im1), (re2, im2), ...] -> [re1, re1, re2, re2, ...]
            LOAD_CV_TO_CV_SPREAD_RE[i] = (i / 2) * 2;

            // Complex vector to complex vector of duplicated image parts:
            // [(re1, im1), (re2, im2), ...] -> [im1, im1, im2, im2, ...]
            LOAD_CV_TO_CV_SPREAD_IM[i] = (i / 2) * 2 + 1;
        }
        SHUFFLE_CV_SPREAD_RE = FloatVector.shuffleFromArray(PFS, LOAD_CV_TO_CV_SPREAD_RE, 0);
        SHUFFLE_CV_SPREAD_IM = FloatVector.shuffleFromArray(PFS, LOAD_CV_TO_CV_SPREAD_IM, 0);
    }

    private float x[];

    @Setup(Level.Trial)
    public void Setup() {
        x = new float[PFS.length() * 2];

        for (int i = 0; i < x.length; i++) {
            x[i] = (float) (Math.random() * 2.0 - 1.0);
        }
    }


    @Benchmark
    public void loadTwice(Blackhole bh) {
        final FloatVector vxre = FloatVector.fromArray(PFS, x, 0, LOAD_CV_TO_CV_SPREAD_RE, 0);
        final FloatVector vxim = FloatVector.fromArray(PFS, x, 0, LOAD_CV_TO_CV_SPREAD_IM, 0);
        bh.consume(vxre);
        bh.consume(vxim);
    }

    @Benchmark
    public void loadAndReshuffle(Blackhole bh) {
        final FloatVector vx = FloatVector.fromArray(PFS, x, 0);
        final FloatVector vxre = vx.rearrange(SHUFFLE_CV_SPREAD_RE);
        final FloatVector vxim = vx.rearrange(SHUFFLE_CV_SPREAD_IM);
        bh.consume(vxre);
        bh.consume(vxim);
    }
}
