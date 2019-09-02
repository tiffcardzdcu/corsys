package com.tiffcardz.corsys.sensor;

public interface EcgBlockCreatedListener {
	void onEcgBlockCreated(EcgSampleGenerator ecgSampleGenerator, EcgBlock ecgBlock);
}
