package com.tiffcardz.corsys.gateway;

public interface EcgBlockCreatedListener {
	void onEcgBlockCreated(EcgSampleGenerator ecgSampleGenerator, EcgBlock ecgBlock);
}
