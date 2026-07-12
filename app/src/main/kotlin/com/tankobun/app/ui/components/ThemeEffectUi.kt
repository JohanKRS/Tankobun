package com.tankobun.app.ui.components

import android.animation.ValueAnimator
import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.clip
import com.tankobun.app.LocalTankobunStyle
import com.tankobun.app.LocalTankobunTokens
import com.tankobun.app.TankobunThemeEffect
import kotlinx.coroutines.isActive

@Composable
internal fun TankobunThemeEffectLayer(
    modifier: Modifier = Modifier,
    shape: Shape,
    animated: Boolean = true,
) {
    val style = LocalTankobunStyle.current
    val tokens = LocalTankobunTokens.current
    val effect = style.effects
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    var timeSeconds by remember(effect.kind) { mutableFloatStateOf(0f) }
    val shouldAnimate = animated && effect.animated && animationsEnabled
    LaunchedEffect(effect.kind, shouldAnimate) {
        if (!shouldAnimate) return@LaunchedEffect
        val startedAt = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { frameTime ->
                timeSeconds = (frameTime - startedAt) / 1_000_000_000f
            }
        }
    }
    val clipped = modifier.clip(shape)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        RuntimeThemeEffect(
            modifier = clipped,
            kind = effect.kind,
            colorA = tokens.topBarBleed,
            colorB = tokens.dockBleed,
            intensity = effect.intensity,
            timeSeconds = timeSeconds,
        )
    } else {
        StaticThemeEffect(
            modifier = clipped,
            colorA = tokens.topBarBleed,
            colorB = tokens.dockBleed,
            intensity = effect.intensity,
        )
    }
}

@Composable
private fun StaticThemeEffect(
    modifier: Modifier,
    colorA: Color,
    colorB: Color,
    intensity: Float,
) {
    Canvas(modifier) {
        drawRect(
            brush = Brush.linearGradient(
                listOf(
                    Color.Transparent,
                    colorA.copy(alpha = intensity * 0.28f),
                    colorB.copy(alpha = intensity * 0.36f),
                    Color.Transparent,
                ),
            ),
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun RuntimeThemeEffect(
    modifier: Modifier,
    kind: TankobunThemeEffect,
    colorA: Color,
    colorB: Color,
    intensity: Float,
    timeSeconds: Float,
) {
    val shader = remember {
        runCatching { RuntimeShader(THEME_EFFECT_SHADER) }
            .onFailure { Log.w(THEME_EFFECT_TAG, "Falling back to the static theme effect", it) }
            .getOrNull()
    }
    if (shader == null) {
        StaticThemeEffect(modifier, colorA, colorB, intensity)
        return
    }
    val brush = remember(shader) { ShaderBrush(shader) }
    Canvas(modifier) {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("time", timeSeconds)
        shader.setFloatUniform("mode", kind.shaderMode())
        shader.setFloatUniform("strength", intensity)
        shader.setColorUniform("colorA", colorA.toArgb())
        shader.setColorUniform("colorB", colorB.toArgb())
        drawRect(brush = brush)
    }
}

private fun TankobunThemeEffect.shaderMode(): Float = when (this) {
    TankobunThemeEffect.GRAIN -> 0f
    TankobunThemeEffect.BUBBLE_WASH -> 1f
    TankobunThemeEffect.HALFTONE -> 2f
    TankobunThemeEffect.SPECULAR -> 3f
    TankobunThemeEffect.AURORA -> 4f
}

private const val THEME_EFFECT_TAG = "TankobunThemeEffect"

private const val THEME_EFFECT_SHADER = """
uniform float2 resolution;
uniform float time;
uniform float mode;
uniform float strength;
layout(color) uniform half4 colorA;
layout(color) uniform half4 colorB;

float hash(float2 p) {
    return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / max(resolution, float2(1.0));
    float mask = 0.0;
    if (mode < 0.5) {
        mask = hash(floor(fragCoord * 0.38)) * 0.32;
    } else if (mode < 1.5) {
        float2 p = uv - float2(0.25 + 0.08 * sin(time * 0.5), 0.5);
        float2 q = uv - float2(0.78, 0.35 + 0.10 * cos(time * 0.42));
        mask = smoothstep(0.42, 0.0, length(p)) + smoothstep(0.32, 0.0, length(q));
    } else if (mode < 2.5) {
        float2 grid = fract(fragCoord / 10.0) - 0.5;
        mask = 1.0 - smoothstep(0.16, 0.29, length(grid));
    } else if (mode < 3.5) {
        float sweep = uv.x + uv.y * 0.34 + time * 0.13;
        mask = smoothstep(0.46, 0.50, fract(sweep)) * (1.0 - smoothstep(0.50, 0.58, fract(sweep)));
    } else {
        float waveA = sin((uv.x * 4.4 + uv.y * 2.1) + time * 0.55);
        float waveB = cos((uv.x * 2.0 - uv.y * 4.8) - time * 0.38);
        mask = 0.5 + 0.25 * waveA + 0.25 * waveB;
    }
    mask = clamp(mask * strength, 0.0, 0.52);
    half4 mixed = mix(colorA, colorB, half(clamp(uv.x + 0.18 * sin(time * 0.25), 0.0, 1.0)));
    return half4(mixed.rgb, half(mask));
}
"""
