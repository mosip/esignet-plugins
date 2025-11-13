package io.mosip.signup.plugin.mosipid.util;

import io.mosip.biometrics.util.ConvertRequestDto;
import io.mosip.biometrics.util.face.FaceEncoder;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.constant.PurposeType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.*;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static io.mosip.biometrics.util.CommonUtil.convertJPEGToJP2UsingOpenCV;

@Component
public class BiometricUtil {

    @Autowired
    private CbeffUtil cbeffUtil;

    @Value("${mosip.signup.idrepo.biometric.compression-ratio:1000}")
    private int faceImageCompressionRatio;

    public String convertBase64JpegToBase64BirXML(String base64Jpeg) throws Exception {
        byte[] jpegImage = Base64Utils.decodeFromString(base64Jpeg);
        byte[] jp2Image = convertJPEGToJP2UsingOpenCV(jpegImage, faceImageCompressionRatio);

        ConvertRequestDto convertRequest = new ConvertRequestDto();
        convertRequest.setVersion("ISO19794_5_2011");
        convertRequest.setPurpose("Registration");
        convertRequest.setImageType(0);
        convertRequest.setInputBytes(jp2Image);
        convertRequest.setModality("Face");
        convertRequest.setCompressionRatio(faceImageCompressionRatio);

        byte[] isoImage = FaceEncoder.convertFaceImageToISO(convertRequest);

        BIR bir = createBIRFromISO(isoImage);

        return Base64Utils.encodeToUrlSafeString(cbeffUtil.createXML(List.of(bir)));
    }

    public BIR createBIRFromISO(byte[] isoImage) {
        BIRInfo birInfo = new BIRInfo.BIRInfoBuilder().withIntegrity(false).build();
        BDBInfo bdbInfo = new BDBInfo.BDBInfoBuilder()
                .withCreationDate(LocalDateTime.now(ZoneOffset.UTC))
                .withType(List.of(BiometricType.FACE))
                .withSubtype(List.of())
                .withPurpose(PurposeType.ENROLL)
                .withLevel(ProcessedLevelType.RAW)
                .withFormat(new RegistryIDType("Mosip", "8"))
                .withQuality(new QualityType(new RegistryIDType("HMAC", "SHA-256"), 0L, null))
                .build();
        BIR bir = new BIR.BIRBuilder()
                .withVersion(new VersionType(1, 1))
                .withCbeffversion(new VersionType(1, 1))
                .withBirInfo(birInfo)
                .withBdb(isoImage)
                .withBdbInfo(bdbInfo)
                .withOthers(null)
                .build();

        return bir;
    }
}
