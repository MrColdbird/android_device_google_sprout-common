$(call inherit-product, device/oukitel/k4000pro/k4000pro.mk)

# Common CM stuff
$(call inherit-product, vendor/cm/config/common_full_phone.mk)

PRODUCT_BUILD_PROP_OVERRIDES += BUILD_FINGERPRINT=6.0/MRA58M/2280749:user/release-keys PRIVATE_BUILD_DESC="k4000pro-user 6.0 MRA58M 2280749 release-keys"

PRODUCT_NAME := cm_k4000pro
PRODUCT_DEVICE :=k4000pro
PRODUCT_BRAND := oukitel
PRODUCT_MANUFACTURER := Oukitel
PRODUCT_MODEL := K4000PRO

PRODUCT_BUILD_PROP_OVERRIDES += \
    PRODUCT_DEVICE="k4000pro"
