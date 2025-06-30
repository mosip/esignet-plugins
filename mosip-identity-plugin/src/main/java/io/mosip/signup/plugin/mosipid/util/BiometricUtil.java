package io.mosip.signup.plugin.mosipid.util;

import io.mosip.biometrics.util.ConvertRequestDto;
import io.mosip.biometrics.util.face.FaceEncoder;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.constant.PurposeType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.*;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.util.Base64Utils;

import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static io.mosip.biometrics.util.CommonUtil.convertJPEGToJP2UsingOpenCV;

public class BiometricUtil {

    public static String convertBase64JpegToBase64BirXML(String base64Jpeg, int imageCompressionRatio) throws Exception {
        byte[] jpegImage = Base64Utils.decodeFromString(base64Jpeg);
        byte[] jp2Image = convertJPEGToJP2UsingOpenCV(jpegImage, imageCompressionRatio);

        ConvertRequestDto convertRequest = new ConvertRequestDto();
        convertRequest.setVersion("ISO19794_5_2011");
        convertRequest.setPurpose("Registration");
        convertRequest.setImageType(0);
        convertRequest.setInputBytes(jp2Image);
        convertRequest.setModality("Face");
        convertRequest.setCompressionRatio(imageCompressionRatio);

        byte[] isoImage = FaceEncoder.convertFaceImageToISO(convertRequest);

        BIR bir = createBIRFromISO(isoImage);

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(BIR.class);
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        marshaller.marshal(bir, result);
        String birXml = sw.toString();

        return Base64Utils.encodeToUrlSafeString(birXml.getBytes(StandardCharsets.UTF_8));
    }

    public static BIR createBIRFromISO(byte[] isoImage) {
        BIRInfo birInfo = new BIRInfo.BIRInfoBuilder().withIntegrity(false).build();
        BDBInfo bdbInfo = new BDBInfo.BDBInfoBuilder()
                .withCreationDate(LocalDateTime.now())
                .withType(List.of(BiometricType.FACE))
                .withPurpose(PurposeType.ENROLL)
                .withLevel(ProcessedLevelType.RAW)
                .withFormat(new RegistryIDType("Mosip", "8"))
                .withQuality(new QualityType(new RegistryIDType("HMAC", "SHA-256"), 0L, null))
                .build();
        BIR faceBir = new BIR.BIRBuilder()
                .withVersion(new VersionType(1, 1))
                .withCbeffversion(new VersionType(1, 1))
                .withBirInfo(birInfo)
                .withBdb(isoImage)
                .withBdbInfo(bdbInfo)
                .withOthers(null)
                .build();

        BIR bir = new BIR.BIRBuilder()
                .withBirInfo(birInfo)
                .build();
        bir.setBirs(List.of(faceBir));
        return bir;
    }
}
