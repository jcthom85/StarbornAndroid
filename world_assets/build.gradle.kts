plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("world_assets")
    dynamicDelivery {
        deliveryType.set("install-time")
    }
}
