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
package org.dataconservancy.nihms.cli;

import org.apache.commons.io.IOUtils;
import org.dataconservancy.nihms.integration.FtpBaseIT;
import org.dataconservancy.nihms.submission.SubmissionEngine;
import org.dataconservancy.pass.deposit.transport.Transport;
import org.dataconservancy.pass.deposit.transport.ftp.FtpTransportHints;
import org.junit.Before;
import org.junit.Test;
import submissions.SubmissionResourceUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class NihmsSubmissionAppFtpIT extends FtpBaseIT {

    private static URI SAMPLE_SUBMISSION_RESOURCE = URI.create("fake:submission1");

    private static String NIHMS_FTP_SUBMISSION_BASE_DIRECTORY = String.format("%s%s",
            FtpBaseIT.FTP_SUBMISSION_BASE_DIRECTORY, SubmissionEngine.BASE_DIRECTORY);

    private Map<String, String> transportHints;

    /**
     * Insure that the classpath resource which embodies a submission is available.
     * Connect and login to the FTP server.
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        transportHints = new HashMap<String, String>() {
            {
                put(Transport.TRANSPORT_SERVER_FQDN, ftpHost);
                put(FtpTransportHints.BASE_DIRECTORY, NIHMS_FTP_SUBMISSION_BASE_DIRECTORY);
                put(Transport.TRANSPORT_PROTOCOL, "ftp");
                put(Transport.TRANSPORT_AUTHMODE, "userpass");
                put(Transport.TRANSPORT_USERNAME, "nihmsftpuser");
                put(Transport.TRANSPORT_PASSWORD, "nihmsftppass");
                put(Transport.TRANSPORT_SERVER_PORT, String.valueOf(ftpPort));
                put(FtpTransportHints.DATA_TYPE, "binary");
                put(FtpTransportHints.USE_PASV, "true");
                put(FtpTransportHints.TRANSFER_MODE, "stream");
            }
        };

        assertNotNull("Unable to resolve " + SAMPLE_SUBMISSION_RESOURCE + " as a classpath resource",
                SubmissionResourceUtil.lookupUri(SAMPLE_SUBMISSION_RESOURCE));
        itUtil.connect();
        itUtil.login();
    }

    /**
     * Asserts that the base directory for the submission does not yet exist (this assertion only works because there
     * are no other tests in this class so far).  It is important to logout because the FTP server is configured to only
     * allow one connection at a time (helps discover connection leaks).
     *
     * Boots up the submission app and performs a submission.  The submission app will connect, login, submit, then
     * logout and disconnect.
     *
     * Connect and login to the FTP server, and verify that the submission base directory is there, and that it contains
     * a single file, presumably the package that was just put there by the app.
     *
     * @throws Exception
     */
    @Test
    public void testSubmissionFromCli() throws Exception {
        assertFalse("Did not expect working directory '" + NIHMS_FTP_SUBMISSION_BASE_DIRECTORY + "' to exist!",
                ftpClient.changeWorkingDirectory(NIHMS_FTP_SUBMISSION_BASE_DIRECTORY));
        itUtil.logout();

        // Copy the sample submission data to a temporary file
        File sampleSubmission = File.createTempFile(this.getClass().getName() + "-", ".json");
        sampleSubmission.deleteOnExit();
        IOUtils.copy(SubmissionResourceUtil.lookupStream(SAMPLE_SUBMISSION_RESOURCE),
                new FileOutputStream(sampleSubmission));

        NihmsSubmissionApp app = new NihmsSubmissionApp(sampleSubmission, transportHints);
        app.run();

        itUtil.connect();
        itUtil.login();

        assertTrue("Unable to change working directory to '" + NIHMS_FTP_SUBMISSION_BASE_DIRECTORY + "'",
                ftpClient.changeWorkingDirectory(NIHMS_FTP_SUBMISSION_BASE_DIRECTORY));

        ftpClient.setUseEPSVwithIPv4(true);
        ftpClient.enterLocalPassiveMode();
        assertEquals(1, ftpClient.listFiles().length);
    }
}
