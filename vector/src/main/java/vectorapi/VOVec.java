/*****************************************************************************
 * Copyright (c) 2014, Lev Serebryakov <lev@serebryakov.spb.ru>
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

package vectorapi;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.Vector;

/**
 * @author Lev Serebryakov
 * @noinspection CStyleArrayDeclaration
 */
@SuppressWarnings({ "PointlessArithmeticExpression", "UnusedDeclaration" })
public final class VOVec {
	/* Missing methods which make sense:
		cs_div_cv
		cs_sub_cv
		cv_10log10
		cv_10log10_rs
		cv_20log10
		cv_20log10_rs
		cv_conjmul_cv
		cv_conjmul_cv_i
		cv_cs_lin_cv_cs
		cv_cs_lin_cv_cs_i
		cv_cs_lin_cv_rs
		cv_cs_lin_cv_rs_i
		cv_cs_lin_rv_cs
		cv_cs_lin_rv_cs_i
		cv_cs_lin_rv_rs
		cv_cs_lin_rv_rs_i
		cv_div_cs
		cv_div_cs_i
		cv_div_cv
		cv_div_cv_i
		cv_dot_cv
		cv_exp
		cv_exp_i
		cv_im
		cv_max
		cv_max_cv
		cv_max_cv_i
		cv_maxarg
		cv_min
		cv_min_cv
		cv_min_cv_i
		cv_minarg
		cv_re
		cv_rev
		cv_rev_i
		cv_rs_lin_cv_rs
		cv_rs_lin_cv_rs_i
		cv_rs_lin_rv_cs
		cv_rs_lin_rv_cs_i
		cv_rs_lin_rv_rs
		cv_rs_lin_rv_rs_i
		cv_sub_cs
		cv_sub_cs_i
		cv_sub_cv
		cv_sub_cv_i
		cv_sub_rs
		cv_sub_rs_i
		cv_sub_rv
		cv_sub_rv_i
		rs_div_cv
		rs_sub_cv
		rs_sub_rv
		rv_20log10
		rv_20log10_i
		rv_20log10_rs
		rv_20log10_rs_i
		rv_conjmul_cv
		rv_cs_lin_rv_cs
		rv_cvt
		rv_div_cv
		rv_min
		rv_min_rv
		rv_min_rv_i
		rv_minarg
		rv_rev
		rv_rev_i
		rv_rs_lin_rv_cs
		rv_sub_cv
		rv_sub_rs
		rv_sub_rs_i
		rv_sub_rv
		rv_sub_rv_i
	 */
	private final static FloatVector.FloatSpecies PFS = FloatVector.preferredSpecies();
	private final static int EPV = PFS.length();
	private final static int EPV2 = EPV / 2;
	private final static Vector.Mask<Float> MASK_C_RE;
	private final static Vector.Mask<Float> MASK_C_IM;
	private final static FloatVector ZERO = PFS.zero();
	private final static int[] LOAD_RV_TO_CV_RE;
	private final static int[] LOAD_RV_TO_CV_BOTH;
	private final static int[] LOAD_CS_TO_CV_SPREAD;
	private final static int[] LOAD_CV_TO_CV_SPREAD_RE;
	private final static int[] LOAD_CV_TO_CV_SPREAD_IM;
	private final static int[] LOAD_CV_TO_CV_PACK_RE;
	private final static int[] LOAD_CV_TO_CV_PACK_IM;
	private final static Vector.Shuffle<Float> SHUFFLE_CV_SWAP_RE_IM;

	static {
		boolean[] alter = new boolean[EPV + 1];
		alter[0] = true;
		for (int i = 1; i < alter.length; i++)
			alter[i] = !alter[i-1];
		MASK_C_RE = FloatVector.maskFromArray(PFS, alter, 0);
		MASK_C_IM = FloatVector.maskFromArray(PFS, alter, 1);

		LOAD_RV_TO_CV_RE = new int[EPV];
		LOAD_RV_TO_CV_BOTH = new int[EPV];
		LOAD_CS_TO_CV_SPREAD = new int[EPV];
		LOAD_CV_TO_CV_SPREAD_RE = new int[EPV];
		LOAD_CV_TO_CV_SPREAD_IM = new int[EPV];
		LOAD_CV_TO_CV_PACK_RE = new int[EPV];
		LOAD_CV_TO_CV_PACK_IM = new int[EPV];
		for (int i = 0; i < EPV; i++) {
			// Load real vector to complex vector's RE part
			// [r1, r2, ...] -> [(r1, 0), (r2, 0), ...]
			if (i % 2 == 0)
				LOAD_RV_TO_CV_RE[i] = i / 2;
			else
				LOAD_RV_TO_CV_RE[i] = 0;

			// Load real vector to complex vector's IM part
			// [r1, r2, ...] -> [(r1, r1), (r2, r2), ...]
			LOAD_RV_TO_CV_BOTH[i] = i / 2;

			// Complex scalar complex to complex vector with spread to each element
			// [re, im] -> [re, im, re, im, re, im, ...]
			if (i % 2 == 0)
				LOAD_CS_TO_CV_SPREAD[i] = 0;
			else
				LOAD_CS_TO_CV_SPREAD[i] = 1;

			// Complex vector to complex vector of duplicated real parts:
			// [(re1, im1), (re2, im2), ...] -> [re1, re1, re2, re2, ...]
			LOAD_CV_TO_CV_SPREAD_RE[i] = (i / 2) * 2;

			// Complex vector to complex vector of duplicated image parts:
			// [(re1, im1), (re2, im2), ...] -> [im1, im1, im2, im2, ...]
			LOAD_CV_TO_CV_SPREAD_IM[i] = (i / 2) * 2 + 1;

			// Vector complex to vector of twice as much real parts
			// [(re1, im1), (re2, im2), ...] -> [re1, re2, ...]
			LOAD_CV_TO_CV_PACK_RE[i] = i * 2;

			// Vector complex to vector of twice as much imaginary parts
			// [(re1, im1), (re2, im2), ...] -> [im1, im2, ...]
			LOAD_CV_TO_CV_PACK_IM[i] = i * 2 + 1;
		}

		int[] shuffle_c_swap_re_im = new int[EPV];
		for (int i = 0; i < shuffle_c_swap_re_im.length; i += 2) {
			shuffle_c_swap_re_im[i + 0] = i + 1;
			shuffle_c_swap_re_im[i + 1] = i + 0;
		}

		SHUFFLE_CV_SWAP_RE_IM = FloatVector.shuffleFromArray(PFS, shuffle_c_swap_re_im, 0);
	}

	public static void rv_add_rs_i(float z[], int zOffset, float x, int count) {
		while (count >= EPV) {
			final FloatVector fz = FloatVector.fromArray(PFS, z, zOffset);
			fz.add(x).intoArray(z, zOffset);
			count -= EPV;
			zOffset += EPV;
		}
		while (count-- > 0)
			z[zOffset++] += x;
	}

	public static void rv_add_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count >= EPV) {
			final FloatVector fz = FloatVector.fromArray(PFS, z, zOffset);
			final FloatVector fx = FloatVector.fromArray(PFS, x, xOffset);
			fz.add(fx).intoArray(z, zOffset);
			count -= EPV;
			xOffset += EPV;
			zOffset += EPV;
		}
		while (count-- > 0)
			z[zOffset++] += x[xOffset++];
	}

	public static void cv_add_rs_i(float z[], int zOffset, float x, int count) {
		zOffset <<= 1;
		while (count >= EPV2) {
			final FloatVector fz = FloatVector.fromArray(PFS, z, zOffset);
			fz.add(x, MASK_C_RE).intoArray(z, zOffset);
			count -= EPV2;
			zOffset += EPV;
		}
		while (count-- > 0) {
			z[zOffset + 0] += x;
			zOffset += 2;
		}
	}

	public static void cv_add_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		while (count >= EPV2) {
			final FloatVector fz = FloatVector.fromArray(PFS, z, zOffset);
			final FloatVector fx = FloatVector.fromArray(PFS, x, xOffset, MASK_C_RE, LOAD_RV_TO_CV_RE, 0);
			fz.add(fx, MASK_C_RE).intoArray(z, zOffset);
			count -= EPV2;
			xOffset += EPV2;
			zOffset += EPV;
		}
		while (count-- > 0) {
			z[zOffset + 0] += x[xOffset++];
			zOffset += 2;
		}
	}

	public static void cv_add_cs_i(float z[], int zOffset, float x[], int count) {
		final FloatVector fx = FloatVector.fromArray(PFS, x, 0, LOAD_CS_TO_CV_SPREAD, 0);
		zOffset <<= 1;
		while (count >= EPV2) {
			final FloatVector fz = FloatVector.fromArray(PFS, z, zOffset);
			fz.add(fx).intoArray(z, zOffset);
			zOffset += EPV;
			count -= EPV2;
		}
		while (count-- > 0) {
			z[zOffset + 0] += x[0];
			z[zOffset + 1] += x[1];
			zOffset += 2;
		}
	}

	public static void cv_add_cv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count >= EPV2) {
			final FloatVector fz = FloatVector.fromArray(PFS, z, zOffset);
			final FloatVector fx = FloatVector.fromArray(PFS, x, xOffset);
			fz.add(fx).intoArray(z, zOffset);
			count -= EPV2;
			xOffset += EPV;
			zOffset += EPV;
		}
		while (count-- > 0) {
			z[zOffset + 0] += x[xOffset + 0];
			z[zOffset + 1] += x[xOffset + 1];
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void rv_add_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		while (count >= EPV) {
			final FloatVector fx = FloatVector.fromArray(PFS, x, xOffset);
			fx.add(y).intoArray(z, zOffset);
			count -= EPV;
			xOffset += EPV;
			zOffset += EPV;
		}
		while (count-- > 0)
			z[zOffset++] = x[xOffset++] + y;
	}

	public static void rv_add_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		while (count >= EPV) {
			final FloatVector fx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector fy = FloatVector.fromArray(PFS, y, yOffset);
			fx.add(fy).intoArray(z, zOffset);
			count -= EPV;
			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
		}
		while (count-- > 0)
			z[zOffset++] = x[xOffset++] + y[yOffset++];
	}

	public static void cv_add_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count >= EPV2) {
			final FloatVector fx = FloatVector.fromArray(PFS, x, xOffset);
			fx.add(y, MASK_C_RE).intoArray(z, zOffset);
			count -= EPV2;
			xOffset += EPV;
			zOffset += EPV;
		}
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] + y;
			z[zOffset + 1] = x[xOffset + 1];
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_add_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count >= EPV2) {
			final FloatVector fx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector fy = FloatVector.fromArray(PFS, y, yOffset, MASK_C_RE, LOAD_RV_TO_CV_RE, 0);
			fx.add(fy, MASK_C_RE).intoArray(z, zOffset);
			count -= EPV2;
			xOffset += EPV;
			yOffset += EPV2;
			zOffset += EPV;
		}
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] + y[yOffset++];
			z[zOffset + 1] = x[xOffset + 1];
			zOffset += 2;
			xOffset += 2;
		}

	}

	public static void cv_add_cs(float z[], int zOffset, float x[], int xOffset, float y[], int count) {
		final FloatVector fy = FloatVector.fromArray(PFS, y, 0, LOAD_CS_TO_CV_SPREAD, 0);
		zOffset <<= 1;
		xOffset <<= 1;
		while (count >= EPV2) {
			final FloatVector fx = FloatVector.fromArray(PFS, x, xOffset);
			fx.add(fy).intoArray(z, zOffset);
			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}
		while (count-- > 0) {
			z[zOffset + 0] += x[xOffset + 0] + y[0];
			z[zOffset + 1] += x[xOffset + 1] + y[1];
			xOffset += 2;
			zOffset += 2;
		}
	}

	public static void cv_add_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		yOffset <<= 1;
		while (count >= EPV2) {
			final FloatVector fx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector fy = FloatVector.fromArray(PFS, y, yOffset);
			fx.add(fy).intoArray(z, zOffset);
			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}
		while (count-- > 0) {
			z[zOffset + 0] += x[xOffset + 0] + y[yOffset + 0];
			z[zOffset + 1] += x[xOffset + 1] + y[yOffset + 1];
			xOffset += 2;
			yOffset += 2;
			zOffset += 2;
		}
	}

	public static void rv_mul_rs_i(float z[], int zOffset, float x, int count) {
		while (count >= EPV) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.mul(x).intoArray(z, zOffset);
			zOffset += EPV;
			count -= EPV;
		}
		while (count-- > 0)
			z[zOffset++] *= x;
	}

	public static void rv_mul_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.mul(vx).intoArray(z, zOffset);
			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}
		while (count-- > 0)
			z[zOffset++] *= x[xOffset++];
	}

	public static void cv_mul_rs_i(float z[], int zOffset, float x, int count) {
		zOffset <<= 1;
		while (count >= EPV2) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.mul(x).intoArray(z, zOffset);
			zOffset += EPV;
			count -= EPV2;
		}
		while (count-- > 0) {
			z[zOffset + 0] *= x;
			z[zOffset + 1] *= x;
			zOffset += 2;
		}
	}

	public static void cv_mul_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset, LOAD_RV_TO_CV_BOTH, 0);
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.mul(vx).intoArray(z, zOffset);
			xOffset += EPV2;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] *= x[xOffset];
			z[zOffset + 1] *= x[xOffset];
			zOffset += 2;
			xOffset += 1;
		}
	}

	public static void cv_mul_cs_i(float z[], int zOffset, float x[], int count) {
		FloatVector vxre = null, vxim = null;
		if (count >= EPV2) {
			vxre = PFS.broadcast(x[0]);
			vxim = PFS.broadcast(x[1]);
		}

		float k0, k1, k2;

		zOffset <<= 1;
		while (count >= EPV2) {
			// Load z
			// vz is [(z[0].re, z[0].im), (z[1].re, z[1].im), ...]
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);

			// Multiply z with re from x
			// It is [(z[0].re * x.re, z[0].im * x.re), (z[1].re * x.re, z[1].im * x.re), ...]
			final FloatVector vmulxre = vz.mul(vxre);
			// Multiply z with vx2
			// It is [(z[0].re * x.im, z[0].im * x.im), (z[1].re * x.im, z[1].im * x.im), ...]
			final FloatVector vmulxim = vz.mul(vxim);
			// Reshuffle second ne in pairs
			// It is [(z[0].im * x.im, z[0].re * x.im), (z[1].im * x.im, z[1].re * x.im), ...]
			final FloatVector vmulximswap = vmulxim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			// Get real parts of result
			// it is ([z[0].re * x.re - z[0].im * x.im, ?], ...)
			final FloatVector vrre = vmulxre.sub(vmulximswap, MASK_C_RE);

			// Get imaginary parts of result
			// it is ([?, z[0].im * x.re + z[0].re * x.im], ...)
			final FloatVector vrim = vmulxre.add(vmulximswap, MASK_C_IM);

			// Blend together & save
			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV2;
		}
		while (count-- > 0) {
			k0 = z[zOffset + 0] * x[0];
			k1 = z[zOffset + 1] * x[1];
			k2 = (z[zOffset + 0] + z[zOffset + 1]) * (x[0] + x[1]);
			z[zOffset + 0] = k0 - k1;
			z[zOffset + 1] = k2 - k0 - k1;
			zOffset += 2;
		}
	}

	public static void cv_mul_cv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		float k0, k1, k2;
		zOffset <<= 1;
		xOffset <<= 1;
		while (count >= EPV2) {
			// @@TODO: Check
			// Load x twice (or better load once and shuffle twice?)
			// vxre is [(x[0].re, x[0].re), (x[1].re, x[1].re), ...]
			final FloatVector vxre = FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_SPREAD_RE, 0);
			// vxim is [(x[0].im, x[0].im), (x[1].im, x[1].im), ...]
			final FloatVector vxim = FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_SPREAD_IM, 0);

			// Load z
			// vz is [(z[0].re, z[0].im), (z[1].re, z[1].im), ...]
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);

			// Multiply z with re from x
			// It is [(z[0].re * x.re, z[0].im * x.re), (z[1].re * x.re, z[1].im * x.re), ...]
			final FloatVector vmulxre = vz.mul(vxre);
			// Multiply z with vx2
			// It is [(z[0].re * x.im, z[0].im * x.im), (z[1].re * x.im, z[1].im * x.im), ...]
			final FloatVector vmulxim = vz.mul(vxim);
			// Reshuffle second ne in pairs
			// It is [(z[0].im * x.im, z[0].re * x.im), (z[1].im * x.im, z[1].re * x.im), ...]
			final FloatVector vmulximswap = vmulxim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			// Get real parts of result
			// it is ([z[0].re * x.re - z[0].im * x.im, ?], ...)
			final FloatVector vrre = vmulxre.sub(vmulximswap, MASK_C_RE);

			// Get imaginary parts of result
			// it is ([?, z[0].im * x.re + z[0].re * x.im], ...)
			final FloatVector vrim = vmulxre.add(vmulximswap, MASK_C_IM);

			// Blend together & save
			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}
		while (count-- > 0) {
			k0 = z[zOffset + 0] * x[xOffset + 0];
			k1 = z[zOffset + 1] * x[xOffset + 1];
			k2 = (z[zOffset + 0] + z[zOffset + 1]) * (x[xOffset + 0] + x[xOffset + 1]);
			z[zOffset + 0] = k0 - k1;
			z[zOffset + 1] = k2 - k0 - k1;
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void rv_mul_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.mul(y).intoArray(z, zOffset);
			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}
		while (count-- > 0)
			z[zOffset++] = x[xOffset++] * y;
	}

	public static void rv_mul_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.mul(vy).intoArray(z, zOffset);
			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}
		while (count-- > 0)
			z[zOffset++] = x[xOffset++] * y[yOffset++];
	}

	public static void cv_mul_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.mul(y).intoArray(z, zOffset);
			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * y;
			z[zOffset + 1] = x[xOffset + 1] * y;
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_mul_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset, LOAD_RV_TO_CV_BOTH, 0);
			vx.mul(vy).intoArray(z, zOffset);
			yOffset += EPV2;
			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * y[yOffset];
			z[zOffset + 1] = x[xOffset + 1] * y[yOffset];
			zOffset += 2;
			xOffset += 2;
			yOffset += 1;
		}
	}

	public static void cv_mul_cs(float z[], int zOffset, float x[], int xOffset, float y[], int count) {
		FloatVector vyre = null, vyim = null;
		if (count >= EPV2) {
			vyre = PFS.broadcast(y[0]);
			vyim = PFS.broadcast(y[1]);
		}

		zOffset <<= 1;
		xOffset <<= 1;
		while (count >= EPV2) {
			// Load x
			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);

			// Multiply x with re from y
			// It is [(x[0].re * y.re, x[0].im * y.re), (x[1].re * y.re, x[1].im * y.re), ...]
			final FloatVector vmulyre = vx.mul(vyre);
			// Multiply z with vx2
			// It is [(x[0].re * y.im, x[0].im * y.im), (x[1].re * y.im, x[1].im * y.im), ...]
			final FloatVector vmulyim = vx.mul(vyim);
			// Reshuffle second ne in pairs
			// It is [(x[0].im * y.im, x[0].re y x.im), (x[1].im * y.im, x[1].re * y.im), ...]
			final FloatVector vmulximswap = vmulyim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			// Get real parts of result
			// it is ([x[0].re * y.re - x[0].im * y.im, ?], ...)
			final FloatVector vrre = vmulyre.sub(vmulximswap, MASK_C_RE);

			// Get imaginary parts of result
			// it is ([?, x[0].im * y.re + x[0].re * y.im], ...)
			final FloatVector vrim = vmulyre.add(vmulximswap, MASK_C_IM);

			// Blend together & save
			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		float k0, k1;
		while (count-- > 0) {
			k0 = x[xOffset + 0] * y[0];
			k1 = x[xOffset + 1] * y[1];
			z[zOffset + 0] = k0 - k1;
			z[zOffset + 1] = (x[xOffset + 0] + x[xOffset + 1]) * (y[0] + y[1]) - k0 - k1;
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_mul_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		yOffset <<= 1;

		zOffset <<= 1;
		while (count >= EPV2) {
			// @@TODO: Check
			// Load y twice (or better load once and shuffle twice?)
			// vyre is [(y[0].re, y[0].re), (y[1].re, y[1].re), ...]
			final FloatVector vyre = FloatVector.fromArray(PFS, y, yOffset, LOAD_CV_TO_CV_SPREAD_RE, 0);
			// vyim is [(y[0].im, y[0].im), (y[1].im, y[1].im), ...]
			final FloatVector vyim = FloatVector.fromArray(PFS, y, yOffset, LOAD_CV_TO_CV_SPREAD_IM, 0);

			// Load x
			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);

			// Multiply x with re from y
			// It is [(x[0].re * y.re, x[0].im * y.re), (x[1].re * y.re, x[1].im * y.re), ...]
			final FloatVector vmulyre = vx.mul(vyre);
			// Multiply z with vx2
			// It is [(x[0].re * y.im, x[0].im * y.im), (x[1].re * y.im, x[1].im * y.im), ...]
			final FloatVector vmulyim = vx.mul(vyim);
			// Reshuffle second ne in pairs
			// It is [(x[0].im * y.im, x[0].re y x.im), (x[1].im * y.im, x[1].re * y.im), ...]
			final FloatVector vmulximswap = vmulyim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			// Get real parts of result
			// it is ([x[0].re * y.re - x[0].im * y.im, ?], ...)
			final FloatVector vrre = vmulyre.sub(vmulximswap, MASK_C_RE);

			// Get imaginary parts of result
			// it is ([?, x[0].im * y.re + x[0].re * y.im], ...)
			final FloatVector vrim = vmulyre.add(vmulximswap, MASK_C_IM);

			// Blend together & save
			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		float k0, k1;
		while (count-- > 0) {
			k0 = x[xOffset + 0] * y[yOffset + 0];
			k1 = x[xOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = k0 - k1;
			z[zOffset + 1] = (x[xOffset + 0] + x[xOffset + 1]) * (y[yOffset + 0] + y[yOffset + 1]) - k0 - k1;
			zOffset += 2;
			xOffset += 2;
			yOffset += 2;
		}
	}

	public static void rv_div_rs_i(float z[], int zOffset, float x, int count) {
		while (count >= EPV) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.div(x).intoArray(z, zOffset);
			zOffset += EPV;
			count -= EPV;
		}
		while (count-- > 0)
			z[zOffset++] /= x;
	}

	public static void rv_div_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.div(vx).intoArray(z, zOffset);
			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}
		while (count-- > 0)
			z[zOffset++] /= x[xOffset++];
	}

	public static void cv_div_rs_i(float z[], int zOffset, float x, int count) {
		zOffset <<= 1;
		while (count >= EPV2) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.div(x).intoArray(z, zOffset);
			zOffset += EPV;
			count -= EPV2;
		}
		while (count-- > 0) {
			z[zOffset + 0] /= x;
			z[zOffset + 1] /= x;
			zOffset += 2;
		}
	}

	public static void cv_div_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset, LOAD_RV_TO_CV_BOTH, 0);
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.div(vx).intoArray(z, zOffset);
			xOffset += EPV2;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] /= x[xOffset];
			z[zOffset + 1] /= x[xOffset];
			zOffset += 2;
			xOffset += 1;
		}
	}

	public static void rv_div_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.div(y).intoArray(z, zOffset);
			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}
		while (count-- > 0)
			z[zOffset++] = x[xOffset++] / y;
	}

	public static void rs_div_rv(float z[], int zOffset, float x, float y[], int yOffset, int count) {
		final FloatVector vx = PFS.broadcast(x);
		while (count >= EPV) {
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.div(vy).intoArray(z, zOffset);
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}
		while (count-- > 0)
			z[zOffset++] = x / y[yOffset++];
	}

	public static void rv_div_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.div(vy).intoArray(z, zOffset);
			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}
		while (count-- > 0)
			z[zOffset++] = x[xOffset++] / y[yOffset++];
	}

	public static void cv_div_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		zOffset <<= 1;
		xOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.div(y).intoArray(z, zOffset);
			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] / y;
			z[zOffset + 1] = x[xOffset + 1] / y;
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_div_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset, LOAD_RV_TO_CV_BOTH, 0);
			vx.div(vy).intoArray(z, zOffset);
			yOffset += EPV2;
			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] / y[yOffset];
			z[zOffset + 1] = x[xOffset + 1] / y[yOffset];
			zOffset += 2;
			xOffset += 2;
			yOffset += 1;
		}
	}

	public static void cv_conj_i(float z[], int zOffset, int count) {
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.neg(MASK_C_IM).intoArray(z, zOffset);
			zOffset += EPV;
			count -= EPV2;
		}

		zOffset++;
		while (count-- > 0) {
			z[zOffset] = -z[zOffset]; // + 1
			zOffset += 2;
		}
	}

	public static void cv_conj(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.neg(MASK_C_IM).intoArray(z, zOffset);
			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0];
			z[zOffset + 1] = -x[xOffset + 1];
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void rv_expi(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset, LOAD_RV_TO_CV_BOTH, 0);
			final FloatVector vzre = vx.cos(MASK_C_RE);
			final FloatVector vzim = vx.sin(MASK_C_IM);
			vzre.blend(vzim, MASK_C_IM).intoArray(z, zOffset);
			xOffset += EPV2;
			zOffset += EPV;
			count -= EPV2;
		}
		while (count-- > 0) {
			z[zOffset + 0] = (float)Math.cos(x[xOffset]);
			z[zOffset + 1] = (float)Math.sin(x[xOffset]);
			zOffset += 2;
			xOffset += 1;
		}
	}

	public static void rv_exp_i(float z[], int zOffset, int count) {
		while (count >= EPV) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.exp().intoArray(z, zOffset);
			zOffset += EPV;
			count -= EPV;
		}
		while (count-- > 0) {
			z[zOffset] = (float)Math.exp(z[zOffset]);
			zOffset++;
		}
	}

	public static void rv_exp(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.exp().intoArray(z, zOffset);
			zOffset += EPV;
			xOffset += EPV;
			count -= EPV;
		}
		while (count-- > 0)
			z[zOffset++] = (float)Math.exp(x[xOffset++]);
	}

	public static void cv_abs(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;

		while(count >= EPV) {
			//@@TODO: CHECK
			// Or load and reshuffle?
			final FloatVector vxre = FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_PACK_RE, 0);
			final FloatVector vxim = FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_PACK_IM, 0);
			vxre.hypot(vxim).intoArray(z, zOffset);
			// We load twice as much complex numbers
			xOffset += EPV * 2;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset++] = (float)Math.hypot(x[xOffset + 0], x[xOffset + 1]);
			xOffset += 2;
		}
	}

	public static void cv_arg(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;

		while(count >= EPV) {
			//@@TODO: CHECK
			// Or load and reshuffle?
			final FloatVector vxre = FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_PACK_RE, 0);
			final FloatVector vxim = FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_PACK_IM, 0);
			vxim.atan2(vxre).intoArray(z, zOffset);
			// We load twice as much complex numbers
			xOffset += EPV * 2;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset++] = (float)Math.atan2(x[xOffset + 1], x[xOffset + 0]);
			xOffset += 2;
		}
	}

	public static void cv_argmul_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		xOffset <<= 1;

		while(count >= EPV) {
			//@@TODO: CHECK
			// Or load and reshuffle?
			final FloatVector vxre = FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_PACK_RE, 0);
			final FloatVector vxim = FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_PACK_IM, 0);
			vxim.atan2(vxre).mul(y).intoArray(z, zOffset);
			// We load twice as much complex numbers
			xOffset += EPV * 2;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset++] = y * (float)Math.atan2(x[xOffset + 1], x[xOffset + 0]);
			xOffset += 2;
		}
	}

	public static void rv_abs_i(float z[], int zOffset, int count) {
		while(count >= EPV) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.abs().intoArray(z, zOffset);
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset] = Math.abs(z[zOffset]);
			zOffset += 1;
		}
	}

	public static void rv_abs(float z[], int zOffset, float x[], int xOffset, int count) {
		while(count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.abs().intoArray(z, zOffset);
			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = Math.abs(x[xOffset++]);
	}

	public static void cv_r2p_i(float z[], int zOffset, int count) {
		zOffset <<= 1;

		while (count >= EPV2) {
			//@@TODO: Check
			// Or load and reshuffle?
			final FloatVector vzre = FloatVector.fromArray(PFS, z, zOffset, LOAD_CV_TO_CV_SPREAD_RE, 0);
			final FloatVector vzim = FloatVector.fromArray(PFS, z, zOffset, LOAD_CV_TO_CV_SPREAD_IM, 0);

			//@@TODO: Check
			// Or better with masks?
			final FloatVector vrre = vzre.hypot(vzim);
			final FloatVector vrim = vzim.atan2(vzre);

			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV2;
		}

		float abs, arg;
		while (count-- > 0) {
			abs = (float)Math.hypot(z[zOffset + 0], z[zOffset + 1]);
			arg = (float)Math.atan2(z[zOffset + 1], z[zOffset + 0]);
			z[zOffset + 0] = abs;
			z[zOffset + 1] = arg;
			zOffset += 2;
		}
	}

	public static void cv_r2p(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;

		while (count >= EPV2) {
			//@@TODO: Check
			// Or load and reshuffle?
			final FloatVector vxre = FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_SPREAD_RE, 0);
			final FloatVector vxim = FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_SPREAD_IM, 0);

			//@@TODO: Check
			// Or better with masks?
			final FloatVector vrre = vxre.hypot(vxim);
			final FloatVector vrim = vxim.atan2(vxre);

			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = (float)Math.hypot(x[xOffset + 0], x[xOffset + 1]);
			z[zOffset + 1] = (float)Math.atan2(x[xOffset + 1], x[xOffset + 0]);
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_p2r_i(float z[], int zOffset, int count) {
		zOffset <<= 1;

		while (count >= EPV2) {
			//@@TODO: Check
			// Or load and reshuffle?
			// Or load with pack and process 2x elements?
			final FloatVector vzre = FloatVector.fromArray(PFS, z, zOffset, LOAD_CV_TO_CV_SPREAD_RE, 0);
			final FloatVector vzim = FloatVector.fromArray(PFS, z, zOffset, LOAD_CV_TO_CV_SPREAD_IM, 0);

			//@@TODO: Check
			// Or better with masks?
			final FloatVector vrre = vzre.mul(vzim.cos());
			final FloatVector vrim = vzre.mul(vzim.sin());

			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV2;
		}

		float re, im;
		while (count-- > 0) {
			re = z[zOffset + 0] * (float)Math.cos(z[zOffset + 1]);
			im = z[zOffset + 0] * (float)Math.sin(z[zOffset + 1]);
			z[zOffset + 0] = re;
			z[zOffset + 1] = im;
			zOffset += 2;
		}
	}

	public static void cv_p2r(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;

		while (count >= EPV2) {
			//@@TODO: Check
			// Or load and reshuffle?
			final FloatVector vxre = FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_SPREAD_RE, 0);
			final FloatVector vxim = FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_SPREAD_IM, 0);

			//@@TODO: Check
			// Or better with masks?
			final FloatVector vrre = vxre.mul(vxim.cos());
			final FloatVector vrim = vxre.mul(vxim.sin());

			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * (float)Math.cos(x[xOffset + 1]);
			z[zOffset + 1] = x[xOffset + 0] * (float)Math.sin(x[xOffset + 1]);
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static float rv_sum(float x[], int xOffset, int count) {
		float sum = 0.0f;

		while (count >= EPV) {
			sum += FloatVector.fromArray(PFS, x, xOffset).addAll();
			xOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			sum += x[xOffset++];
		return sum;
	}

	public static void cv_sum(float z[], float x[], int xOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			re += vx.addAll(MASK_C_RE);
			im += vx.addAll(MASK_C_IM);
			xOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			re += x[xOffset + 0];
			im += x[xOffset + 1];
			xOffset += 2;
		}
		z[0] = re;
		z[1] = im;
	}

	public static void cv_sum(float z[], int zOffset, float x[], int xOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			//@WARNING@ MASKS ARE SWAPPED! IS IT JDK PROBLEM?
			re += vx.addAll(MASK_C_IM);
			im += vx.addAll(MASK_C_RE);
			xOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			re += x[xOffset + 0];
			im += x[xOffset + 1];
			xOffset += 2;
		}
		z[(zOffset << 1) + 0] = re;
		z[(zOffset << 1) + 1] = im;
	}

	public static float rv_dot_rv(float x[], int xOffset, float y[], int yOffset, int count) {
		float sum = 0.0f;

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			sum += vx.mul(vy).addAll();
			count -= EPV;
			xOffset += EPV;
			yOffset += EPV;
		}

		while (count-- > 0)
			sum += x[xOffset++] * y[yOffset++];
		return sum;
	}

	public static void rv_dot_cv(float z[], float x[], int xOffset, float y[], int yOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		yOffset <<= 1;

		while (count >= EPV) {
			//@@TODO: Check
			// Maybe, load vy once and reshuffle?
			// Or load real argument with spread and call addAll() twice with mask?
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vyre = FloatVector.fromArray(PFS, y, yOffset, LOAD_CV_TO_CV_PACK_RE, 0);
			final FloatVector vyim = FloatVector.fromArray(PFS, y, yOffset, LOAD_CV_TO_CV_PACK_IM, 0);
			re += vx.mul(vyre).addAll();
			im += vx.mul(vyim).addAll();
			xOffset += EPV;
			yOffset += EPV * 2; // We load twice as much complex numbers
			count -= EPV;
		}

		while (count-- > 0) {
			re += x[xOffset] * y[yOffset + 0];
			im += x[xOffset] * y[yOffset + 1];
			xOffset += 1;
			yOffset += 2;
		}
		z[0] = re;
		z[1] = im;
	}

	public static float rv_max(float x[], int xOffset, int count) {
		float max = Float.NEGATIVE_INFINITY;

		while (count >= EPV) {
			float localMax = FloatVector.fromArray(PFS, x, xOffset).maxAll();
			if (max < localMax)
				max = localMax;
			xOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			if (max < x[xOffset])
				max = x[xOffset];
			xOffset += 1;
		}
		return max;
	}

	public static void rv_max_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.max(vx).intoArray(z, zOffset);
			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			if (x[xOffset] > z[zOffset])
				z[zOffset] = x[xOffset];
			zOffset += 1;
			xOffset += 1;
		}
	}

	public static void rv_max_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.max(vy).intoArray(z, zOffset);
			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset] = x[xOffset] > y[yOffset] ? x[xOffset] : y[yOffset];
			zOffset += 1;
			xOffset += 1;
			yOffset += 1;
		}
	}

	public static int rv_maxarg(float x[], int xOffset, int count) {
		float max = Float.NEGATIVE_INFINITY;
		int i = -1;

		while (count >= EPV) {
			float localMax = FloatVector.fromArray(PFS, x, xOffset).maxAll();
			if (max < localMax) {
				max = localMax;
				i = xOffset;
			}
			xOffset += EPV;
			count -= EPV;
		}

		// Find max in vector
		if (i >= 0) {
			int i2 = i;
			for (int j = i; j < i + EPV; j++) {
				// We could compare here, as max got from here
				if (max == x[j]) {
					i2 = j;
					break;
				}
			}
			i = i2;
		}

		while (count-- > 0) {
			if (max < x[xOffset]) {
				max = x[xOffset];
				i = xOffset;
			}
			xOffset += 1;
		}
		return i;
	}

	public static void rv_rs_lin_rv_rs_i(float z[], int zOffset, float a1, float x[], int xOffset, float a2, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.mul(a1).add(vx.mul(a2)).intoArray(z, zOffset);
			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset] = z[zOffset] * a1 + x[xOffset] * a2;
			zOffset += 1;
			xOffset += 1;
		}
	}

	public static void rv_rs_lin_rv_rs(float z[], int zOffset, float x[], int xOffset, float a1, float y[], int yOffset, float a2, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.mul(a1).add(vy.mul(a2)).intoArray(z, zOffset);
			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = x[xOffset++] * a1 + y[yOffset++] * a2;
	}

	public static void rv_10log10_i(float z[], int zOffset, int count) {
		while (count >= EPV) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.abs().add(Float.MIN_NORMAL).log10().mul(10.0f).intoArray(z, zOffset);
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset] = 10 * (float)Math.log10(Math.abs(z[zOffset]) + Float.MIN_NORMAL);
			zOffset += 1;
		}
	}

	public static void rv_10log10(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.abs().add(Float.MIN_NORMAL).log10().mul(10.0f).intoArray(z, zOffset);
			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = 10 * (float)Math.log10(Math.abs(x[xOffset++]) + Float.MIN_NORMAL);
	}

	public static void rv_10log10_rs_i(float z[], int zOffset, float base, int count) {
		base = 10 * (float)Math.log10(Math.abs(base) + Float.MIN_NORMAL);

		while (count >= EPV) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.abs().add(Float.MIN_NORMAL).log10().mul(10.0f).sub(base).intoArray(z, zOffset);
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset] = 10 * (float)Math.log10(Math.abs(z[zOffset]) + Float.MIN_NORMAL) - base;
			zOffset += 1;
		}
	}

	public static void rv_10log10_rs(float z[], int zOffset, float x[], int xOffset, float base, int count) {
		base = 10 * (float)Math.log10(Math.abs(base) + Float.MIN_NORMAL);

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.abs().add(Float.MIN_NORMAL).log10().mul(10.0f).sub(base).intoArray(z, zOffset);
			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = 10 * (float)Math.log10(Math.abs(x[xOffset++]) + Float.MIN_NORMAL) - base;
	}
}
