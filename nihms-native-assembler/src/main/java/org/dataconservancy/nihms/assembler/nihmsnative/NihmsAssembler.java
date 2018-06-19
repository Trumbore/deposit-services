/*
 * Copyright 2017 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataconservancy.nihms.assembler.nihmsnative;

import org.dataconservancy.nihms.assembler.MetadataBuilder;
import org.dataconservancy.nihms.assembler.PackageStream;
import org.dataconservancy.nihms.model.DepositFile;
import org.dataconservancy.nihms.model.DepositFileType;
import org.dataconservancy.nihms.model.DepositSubmission;
import org.dataconservancy.pass.deposit.assembler.shared.AbstractAssembler;
import org.dataconservancy.pass.deposit.assembler.shared.DepositFileResource;
import org.dataconservancy.pass.deposit.assembler.shared.MetadataBuilderFactory;
import org.dataconservancy.pass.deposit.assembler.shared.ResourceBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NihmsAssembler extends AbstractAssembler {

    /**
     * Package specification URI identifying the NIHMS native packaging spec, as specified by their 07/2017
     * bulk publishing pdf.
     */
    public static final String SPEC_NIHMS_NATIVE_2017_07 = "nihms-native-2017-07";

    /**
     * Mime type of zip files.
     */
    public static final String APPLICATION_GZIP = "application/gzip";

    @Autowired
    public NihmsAssembler(MetadataBuilderFactory mbf, ResourceBuilderFactory rbf) {
        super(mbf, rbf);
    }

    @Override
    protected PackageStream createPackageStream(DepositSubmission submission,
                                                List<DepositFileResource> custodialResources, MetadataBuilder mb,
                                                ResourceBuilderFactory rbf) {
        mb.spec(SPEC_NIHMS_NATIVE_2017_07);
        mb.archive(PackageStream.ARCHIVE.TAR);
        mb.archived(true);
        mb.compressed(true);
        mb.compression(PackageStream.COMPRESSION.GZIP);
        mb.mimeType(APPLICATION_GZIP);

        NihmsZippedPackageStream stream = new NihmsZippedPackageStream(submission, custodialResources, mb, rbf);
        stream.setManifestSerializer(new NihmsManifestSerializer(submission.getManifest()));
        stream.setMetadataSerializer(new NihmsMetadataSerializer(submission.getMetadata()));
        return stream;
    }

}
