package com.entropy.client.renderer;

import com.entropy.GatewayGunMod;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public class GatewayShaders extends RenderLayer {
	
	public static net.minecraft.client.gl.ShaderProgram gatewayShader = null;
	public static net.minecraft.client.gl.ShaderProgram gatewayCoreShader = null;
	
	public static final ShaderProgram gatewayShard = new ShaderProgram(() -> gatewayShader);
	public static final ShaderProgram gatewayCoreShard = new ShaderProgram(() -> gatewayCoreShader);

	public GatewayShaders(String name, VertexFormat format, VertexFormat.DrawMode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
		super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
	}
	
	public static RenderLayer gateway() {
		MultiPhaseParameters rendertype$state = MultiPhaseParameters.builder()
				.program(gatewayShard)
				.texture(new Texture(GatewayGunMod.id("textures/entity/gateway.png"), false, false))
				.transparency(TRANSLUCENT_TRANSPARENCY)
				.cull(DISABLE_CULLING)
				.writeMaskState(COLOR_MASK)
				.overlay(ENABLE_OVERLAY_COLOR)
				.build(true);
		return of("gateway", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, DEFAULT_BUFFER_SIZE, true, true, rendertype$state);
	}
	
	public static RenderLayer gatewayCore() {
		MultiPhaseParameters rendertype$state = MultiPhaseParameters.builder()
				.program(gatewayCoreShard)
				.transparency(NO_TRANSPARENCY)
				.cull(ENABLE_CULLING)
				.overlay(ENABLE_OVERLAY_COLOR)
				.lightmap(DISABLE_LIGHTMAP)
				.texture(new Texture(GatewayGunMod.id("textures/item/gatewaycore.png"), false, false))
				.layering(VIEW_OFFSET_Z_LAYERING)
				.build(true);
		return of("gatewaycore", VertexFormats.POSITION_TEXTURE_COLOR_NORMAL, VertexFormat.DrawMode.QUADS, DEFAULT_BUFFER_SIZE, true, false, rendertype$state);
	}

}
