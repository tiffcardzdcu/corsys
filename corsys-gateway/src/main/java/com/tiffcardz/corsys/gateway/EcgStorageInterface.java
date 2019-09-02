package com.tiffcardz.corsys.gateway;

public interface EcgStorageInterface {
	public void saveEcgBlock(EcgBlock ecgBlock);
	public EcgBlock getOldestUntransmittedEcgBlock();
}
