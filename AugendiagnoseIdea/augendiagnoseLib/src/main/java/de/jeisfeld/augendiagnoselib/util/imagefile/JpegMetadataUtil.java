package de.jeisfeld.augendiagnoselib.util.imagefile;

import android.media.ExifInterface;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.adobe.xmp.XMPException;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.IImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.jpeg.xmp.JpegXmpRewriter;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.MicrosoftTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffDirectoryType;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoShort;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.imaging.util.IoUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jeisfeld.augendiagnoselib.Application;
import de.jeisfeld.augendiagnoselib.R;
import de.jeisfeld.augendiagnoselib.util.PreferenceUtil;

/**
 * Helper clase to retrieve and save metadata in a JPEG file.
 */
public final class JpegMetadataUtil {

	/**
	 * Hide default constructor.
	 */
	private JpegMetadataUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Log all Exif data of the file.
	 *
	 * @param imageFile the image file.
	 * @throws ImageReadException thrown if the metadata cannot be read.
	 * @throws IOException        thrown in case of other errors while reading metadata.
	 */
	public static void printAllExifData(@NonNull final File imageFile) throws ImageReadException, IOException {
		final IImageMetadata metadata = Imaging.getMetadata(imageFile);

		TiffImageMetadata tiffImageMetadata;
		if (metadata instanceof JpegImageMetadata) {
			tiffImageMetadata = ((JpegImageMetadata) metadata).getExif();
		}
		else if (metadata instanceof TiffImageMetadata) {
			tiffImageMetadata = (TiffImageMetadata) metadata;
		}
		else {
			return;
		}

		@SuppressWarnings("unchecked")
		List<TiffImageMetadata.Item> items = (List<TiffImageMetadata.Item>) tiffImageMetadata.getItems();

		for (TiffImageMetadata.Item item : items) {
			Log.i(Application.TAG, item.getTiffField().toString());
		}
	}

	/**
	 * Get the Exif date of an image file.
	 *
	 * @param imageFile the image file.
	 * @return The EXIF date.
	 * @throws ImageReadException thrown if the metadata cannot be read.
	 * @throws IOException        thrown in case of other errors while reading metadata.
	 */
	public static String getExifDate(@NonNull final File imageFile) throws ImageReadException, IOException {
		final IImageMetadata metadata = Imaging.getMetadata(imageFile);
		TiffImageMetadata tiffImageMetadata;
		if (metadata instanceof JpegImageMetadata) {
			tiffImageMetadata = ((JpegImageMetadata) metadata).getExif();
		}
		else if (metadata instanceof TiffImageMetadata) {
			tiffImageMetadata = (TiffImageMetadata) metadata;
		}
		else {
			return null;
		}

		TiffField dateTime = tiffImageMetadata.findField(TiffTagConstants.TIFF_TAG_DATE_TIME);
		if (dateTime == null) {
			return null;
		}
		else {
			return dateTime.getStringValue();
		}
	}

	/**
	 * Retrieve the orientation of a file from the EXIF data. Required, as built-in ExifInterface is not always
	 * reliable.
	 *
	 * @param imageFile the image file.
	 * @return the orientation value.
	 */
	protected static int getExifOrientation(@NonNull final File imageFile) {
		try {
			final IImageMetadata metadata = Imaging.getMetadata(imageFile);
			TiffImageMetadata tiffImageMetadata;

			if (metadata instanceof JpegImageMetadata) {
				tiffImageMetadata = ((JpegImageMetadata) metadata).getExif();
			}
			else if (metadata instanceof TiffImageMetadata) {
				tiffImageMetadata = (TiffImageMetadata) metadata;
			}
			else {
				return ExifInterface.ORIENTATION_UNDEFINED;
			}

			TiffField field = tiffImageMetadata.findField(TiffTagConstants.TIFF_TAG_ORIENTATION);
			if (field != null) {
				return field.getIntValue();
			}
			else {
				TagInfo tagInfo = new TagInfoShort("Orientation", 274, 1, TiffDirectoryType.TIFF_DIRECTORY_IFD0); // MAGIC_NUMBER
				field = tiffImageMetadata.findField(tagInfo);
				if (field != null) {
					return field.getIntValue();
				}
				else {
					return ExifInterface.ORIENTATION_UNDEFINED;
				}
			}
		}
		catch (Exception e) {
			return ExifInterface.ORIENTATION_UNDEFINED;
		}
	}

	/**
	 * Log all XML data of the file.
	 *
	 * @param imageFile the file.
	 * @throws ImageReadException thrown if the metadata cannot be read.
	 * @throws IOException        thrown in case of other errors while reading metadata.
	 * @throws XMPException       thrown in case of issues with XML handling.
	 */
	public static void printAllXmpData(@NonNull final File imageFile) throws ImageReadException, IOException, XMPException {
		final String xmpString = Imaging.getXmpXml(imageFile);
		Log.i(Application.TAG, new XmpHandler(xmpString).getXmpString());
	}

	/**
	 * Validate that the file is a JPEG file.
	 *
	 * @param jpegImageFileName the file to be validated.
	 * @throws IOException thrown if the file is no jpg.
	 */
	public static void checkJpeg(@Nullable final String jpegImageFileName) throws IOException {
		if (jpegImageFileName == null) {
			throw new IOException("Error in checkJpeg - no image passed.");
		}
		File file = new File(jpegImageFileName);
		String mimeType;
		try {
			int dotIndex = jpegImageFileName.lastIndexOf(".");
			if (dotIndex < 0) {
				throw new IOException("File " + jpegImageFileName + " has no valid extension");
			}
			String extension = jpegImageFileName.substring(dotIndex + 1);
			if (!"image/jpeg".equals(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension))) {
				throw new IOException("Bad extension " + extension + " - can handle metadata only for image/jpeg.");
			}

			mimeType = Imaging.getImageInfo(file).getMimeType();
			if (!"image/jpeg".equals(mimeType)) {
				throw new IOException("Bad MIME type " + mimeType + " - can handle metadata only for image/jpeg.");
			}
		}
		catch (ImageReadException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Retrieve the relevant metadata of an image file.
	 *
	 * @param jpegImageFileName the file for which metadata should be retrieved.
	 * @return the metadata of the file.
	 * @throws ImageReadException thrown if the metadata cannot be read.
	 * @throws IOException        thrown in case of other errors while reading metadata.
	 */
	@NonNull
	public static JpegMetadata getMetadata(@NonNull final String jpegImageFileName) throws ImageReadException, IOException {
		checkJpeg(jpegImageFileName);
		JpegMetadata result = new JpegMetadata();
		final File imageFile = new File(jpegImageFileName);

		// Retrieve XMP data
		String xmpString = Imaging.getXmpXml(imageFile);
		XmpHandler parser = new XmpHandler(xmpString);

		// Standard fields are pre-filled with custom data
		result.setTitle(parser.getJeItem(XmpHandler.ITEM_TITLE));
		result.setDescription(parser.getJeItem(XmpHandler.ITEM_DESCRIPTION));
		result.setSubject(parser.getJeItem(XmpHandler.ITEM_SUBJECT));
		result.setComment(parser.getJeItem(XmpHandler.ITEM_COMMENT));
		result.setPerson(parser.getJeItem(XmpHandler.ITEM_PERSON));

		result.setXCenter(parser.getJeItem(XmpHandler.ITEM_X_CENTER));
		result.setYCenter(parser.getJeItem(XmpHandler.ITEM_Y_CENTER));
		result.setOverlayScaleFactor(parser.getJeItem(XmpHandler.ITEM_OVERLAY_SCALE_FACTOR));
		result.setXPosition(parser.getJeItem(XmpHandler.ITEM_X_POSITION));
		result.setYPosition(parser.getJeItem(XmpHandler.ITEM_Y_POSITION));
		result.setZoomFactor(parser.getJeItem(XmpHandler.ITEM_ZOOM_FACTOR));
		result.setOrganizeDate(parser.getJeDate(XmpHandler.ITEM_ORGANIZE_DATE));
		result.setRightLeft(parser.getJeItem(XmpHandler.ITEM_RIGHT_LEFT));
		result.setBrightness(parser.getJeItem(XmpHandler.ITEM_BRIGHTNESS));
		result.setContrast(parser.getJeItem(XmpHandler.ITEM_CONTRAST));
		result.setSaturation(parser.getJeItem(XmpHandler.ITEM_SATURATION));
		result.setColorTemperature(parser.getJeItem(XmpHandler.ITEM_COLOR_TEMPERATURE));
		result.setOverlayColor(parser.getJeItem(XmpHandler.ITEM_OVERLAY_COLOR));
		result.setPupilSize(parser.getJeItem(XmpHandler.ITEM_PUPIL_SIZE));
		result.setPupilXOffset(parser.getJeItem(XmpHandler.ITEM_PUPIL_X_OFFSET));
		result.setPupilYOffset(parser.getJeItem(XmpHandler.ITEM_PUPIL_Y_OFFSET));
		result.setFlags(parser.getJeInt(XmpHandler.ITEM_FLAGS));
		result.setOrientation((short) getExifOrientation(imageFile));

		// For standard fields, use custom data only if there is no other data.
		if (result.getDescription() == null) {
			result.setDescription(parser.getDcDescription());
		}
		if (result.getSubject() == null) {
			result.setSubject(parser.getDcSubject());
		}
		if (result.getPerson() == null) {
			result.setPerson(parser.getMicrosoftPerson());
		}
		if (result.getTitle() == null) {
			result.setTitle(parser.getDcTitle());
		}
		if (result.getComment() == null) {
			result.setComment(parser.getUserComment());
		}

		// Retrieve EXIF data
		try {
			final IImageMetadata metadata = Imaging.getMetadata(imageFile);

			TiffImageMetadata tiffImageMetadata;
			if (metadata instanceof JpegImageMetadata) {
				tiffImageMetadata = ((JpegImageMetadata) metadata).getExif();
			}
			else if (metadata instanceof TiffImageMetadata) {
				tiffImageMetadata = (TiffImageMetadata) metadata;
			}
			else {
				return result;
			}

			// EXIF data have precedence only if saving EXIF is allowed
			TiffField title = tiffImageMetadata.findField(TiffTagConstants.TIFF_TAG_IMAGE_DESCRIPTION);
			if (title != null && (changeExifAllowed() || result.getTitle() == null)) {
				result.setTitle(title.getStringValue().trim());
			}
			String exifComment = null;
			TiffField comment = tiffImageMetadata.findField(ExifTagConstants.EXIF_TAG_USER_COMMENT);
			if (comment != null && comment.getStringValue().trim().length() > 0) {
				exifComment = comment.getStringValue().trim();
			}
			TiffField comment2 = tiffImageMetadata.findField(MicrosoftTagConstants.EXIF_TAG_XPCOMMENT);
			if (comment2 != null && comment2.getStringValue().trim().length() > 0) {
				// XPComment takes precedence if existing
				exifComment = comment2.getStringValue().trim();
			}
			if (exifComment != null && (changeExifAllowed() || result.getComment() == null)) {
				result.setComment(exifComment);
			}
			TiffField subject = tiffImageMetadata.findField(MicrosoftTagConstants.EXIF_TAG_XPSUBJECT);
			if (subject != null && (changeExifAllowed() || result.getSubject() == null)) {
				result.setSubject(subject.getStringValue().trim());
			}
		}
		catch (Exception e) {
			Log.w(Application.TAG, "Error when retrieving Exif data", e);
		}

		// If fields are still null, try to get them from custom XMP
		if (result.getDescription() == null) {
			result.setDescription(parser.getJeItem(XmpHandler.ITEM_DESCRIPTION));
		}
		if (result.getSubject() == null) {
			result.setSubject(parser.getJeItem(XmpHandler.ITEM_SUBJECT));
		}
		if (result.getPerson() == null) {
			result.setPerson(parser.getJeItem(XmpHandler.ITEM_PERSON));
		}
		if (result.getTitle() == null) {
			result.setTitle(parser.getJeItem(XmpHandler.ITEM_TITLE));
		}
		if (result.getComment() == null) {
			result.setComment(parser.getJeItem(XmpHandler.ITEM_COMMENT));
		}

		return result;
	}

	/**
	 * Change metadata of the image (EXIF and XMP as far as applicable).
	 *
	 * @param jpegImageFileName the file for which metadata should be changed.
	 * @param metadata          the new metadata.
	 * @throws ImageReadException  thrown if the metadata cannot be read.
	 * @throws ImageWriteException thrown if the metadata cannot be written.
	 * @throws IOException         thrown in case of other errors while reading metadata.
	 * @throws XMPException        thrown in case of issues with XML handling.
	 */
	public static void changeMetadata(@NonNull final String jpegImageFileName, @NonNull final JpegMetadata metadata) throws IOException,
			ImageReadException, ImageWriteException, XMPException {
		if (changeJpegAllowed()) {
			checkJpeg(jpegImageFileName);
			changeXmpMetadata(jpegImageFileName, metadata);

			if (changeExifAllowed()) {
				try {
					changeExifMetadata(jpegImageFileName, metadata);
				}
				catch (Exception e) {
					throw new ExifStorageException(e);
				}
			}
		}
	}

	/**
	 * Change the EXIF metadata.
	 *
	 * @param jpegImageFileName the file for which metadata should be changed.
	 * @param metadata          the new metadata
	 * @throws ImageReadException  thrown if the metadata cannot be read.
	 * @throws ImageWriteException thrown if the metadata cannot be written.
	 * @throws IOException         thrown in case of other errors while reading metadata.
	 */
	@SuppressWarnings("resource")
	private static void changeExifMetadata(@NonNull final String jpegImageFileName, @NonNull final JpegMetadata metadata)
			throws IOException, ImageReadException, ImageWriteException {
		File jpegImageFile = new File(jpegImageFileName);
		File tempFile = FileUtil.getTempFile(jpegImageFile);

		verifyTempFile(tempFile);

		OutputStream os = null;
		try {
			TiffOutputSet outputSet = null;

			// note that metadata might be null if no metadata is found.
			final IImageMetadata imageMetadata = Imaging.getMetadata(jpegImageFile);
			final JpegImageMetadata jpegMetadata = (JpegImageMetadata) imageMetadata;
			if (jpegMetadata != null) {
				// note that exif might be null if no Exif metadata is found.
				final TiffImageMetadata exif = jpegMetadata.getExif();

				if (exif != null) {
					outputSet = exif.getOutputSet();
				}
			}

			if (outputSet == null) {
				outputSet = new TiffOutputSet();
			}

			final TiffOutputDirectory rootDirectory = outputSet.getOrCreateRootDirectory();
			final TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();

			if (metadata.getTitle() != null) {
				rootDirectory.removeField(MicrosoftTagConstants.EXIF_TAG_XPTITLE);
				rootDirectory.add(MicrosoftTagConstants.EXIF_TAG_XPTITLE, metadata.getTitle());

				rootDirectory.removeField(TiffTagConstants.TIFF_TAG_IMAGE_DESCRIPTION);
				rootDirectory.add(TiffTagConstants.TIFF_TAG_IMAGE_DESCRIPTION, metadata.getTitle());
			}

			if (metadata.getComment() != null) {
				rootDirectory.removeField(MicrosoftTagConstants.EXIF_TAG_XPCOMMENT);
				rootDirectory.add(MicrosoftTagConstants.EXIF_TAG_XPCOMMENT, metadata.getComment());
				exifDirectory.removeField(ExifTagConstants.EXIF_TAG_USER_COMMENT);
				exifDirectory.add(ExifTagConstants.EXIF_TAG_USER_COMMENT, metadata.getComment());
			}

			if (metadata.getSubject() != null) {
				rootDirectory.removeField(MicrosoftTagConstants.EXIF_TAG_XPSUBJECT);
				rootDirectory.add(MicrosoftTagConstants.EXIF_TAG_XPSUBJECT, metadata.getSubject());
			}

			if (metadata.getOrientation() != null) {
				rootDirectory.removeField(TiffTagConstants.TIFF_TAG_ORIENTATION);
				rootDirectory.add(TiffTagConstants.TIFF_TAG_ORIENTATION, metadata.getOrientation());
			}

			int retryCount = 0;
			do {
				try {
					os = new FileOutputStream(tempFile);
					os = new BufferedOutputStream(os);
					new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os, outputSet);
				}
				catch (Exception e) {
					Log.w(Application.TAG, "Error storing EXIF data lossless - try lossy approach");
					os = new FileOutputStream(tempFile);
					os = new BufferedOutputStream(os);
					new ExifRewriter().updateExifMetadataLossy(jpegImageFile, os, outputSet);
				}
				IoUtils.closeQuietly(true, os);
				retryCount++;
			}
			while (tempFile.length() == 0 && retryCount < 2);

			if (!FileUtil.moveFile(tempFile, jpegImageFile)) {
				throw new IOException("Failed to rename file " + tempFile.getAbsolutePath() + " to "
						+ jpegImageFileName);
			}
		}
		finally {
			IoUtils.closeQuietly(false, os);
		}

	}

	/**
	 * Change the XMP metadata.
	 *
	 * @param jpegImageFileName the file for which metadata should be changed.
	 * @param metadata          the new metadata.
	 * @throws ImageReadException  thrown if the metadata cannot be read.
	 * @throws ImageWriteException thrown if the metadata cannot be written.
	 * @throws IOException         thrown in case of other errors while reading metadata.
	 * @throws XMPException        thrown in case of issues with XML handling.
	 */
	@SuppressWarnings("resource")
	private static void changeXmpMetadata(@NonNull final String jpegImageFileName, @NonNull final JpegMetadata metadata)
			throws IOException,
			ImageReadException, ImageWriteException, XMPException {
		File jpegImageFile = new File(jpegImageFileName);
		File tempFile = FileUtil.getTempFile(jpegImageFile);

		verifyTempFile(tempFile);

		OutputStream os = null;
		try {
			final String xmpString = Imaging.getXmpXml(jpegImageFile);

			XmpHandler parser = new XmpHandler(xmpString);

			if (changeExifAllowed()) {
				// Change standard fields only if EXIF allowed
				parser.setDcTitle(metadata.getTitle());
				parser.setDcDescription(metadata.getDescription());
				parser.setDcSubject(metadata.getSubject());
				parser.setUserComment(metadata.getComment());
				parser.setMicrosoftPerson(metadata.getPerson());
			}

			parser.setJeItem(XmpHandler.ITEM_TITLE, metadata.getTitle());
			parser.setJeItem(XmpHandler.ITEM_DESCRIPTION, metadata.getDescription());
			parser.setJeItem(XmpHandler.ITEM_SUBJECT, metadata.getSubject());
			parser.setJeItem(XmpHandler.ITEM_COMMENT, metadata.getComment());
			parser.setJeItem(XmpHandler.ITEM_PERSON, metadata.getPerson());

			parser.setJeItem(XmpHandler.ITEM_X_CENTER, metadata.getXCenterString());
			parser.setJeItem(XmpHandler.ITEM_Y_CENTER, metadata.getYCenterString());
			parser.setJeItem(XmpHandler.ITEM_OVERLAY_SCALE_FACTOR, metadata.getOverlayScaleFactorString());
			parser.setJeItem(XmpHandler.ITEM_X_POSITION, metadata.getXPositionString());
			parser.setJeItem(XmpHandler.ITEM_Y_POSITION, metadata.getYPositionString());
			parser.setJeItem(XmpHandler.ITEM_ZOOM_FACTOR, metadata.getZoomFactorString());
			parser.setJeDate(XmpHandler.ITEM_ORGANIZE_DATE, metadata.getOrganizeDate());
			parser.setJeItem(XmpHandler.ITEM_RIGHT_LEFT, metadata.getRightLeftString());
			parser.setJeItem(XmpHandler.ITEM_BRIGHTNESS, metadata.getBrightnessString());
			parser.setJeItem(XmpHandler.ITEM_CONTRAST, metadata.getContrastString());
			parser.setJeItem(XmpHandler.ITEM_SATURATION, metadata.getSaturationString());
			parser.setJeItem(XmpHandler.ITEM_COLOR_TEMPERATURE, metadata.getColorTemperatureString());
			parser.setJeItem(XmpHandler.ITEM_OVERLAY_COLOR, metadata.getOverlayColorString());
			parser.setJeItem(XmpHandler.ITEM_PUPIL_SIZE, metadata.getPupilSizeString());
			parser.setJeItem(XmpHandler.ITEM_PUPIL_X_OFFSET, metadata.getPupilXOffsetString());
			parser.setJeItem(XmpHandler.ITEM_PUPIL_Y_OFFSET, metadata.getPupilYOffsetString());
			parser.setJeInt(XmpHandler.ITEM_FLAGS, metadata.getFlags());

			int retryCount = 0;
			do {
				os = new FileOutputStream(tempFile);
				os = new BufferedOutputStream(os);

				new JpegXmpRewriter().updateXmpXml(jpegImageFile, os, parser.getXmpString());

				IoUtils.closeQuietly(true, os);
				retryCount++;
			}
			while (tempFile.length() == 0 && retryCount < 2);

			if (!FileUtil.moveFile(tempFile, jpegImageFile)) {
				throw new IOException("Failed to rename file " + tempFile.getAbsolutePath() + " to "
						+ jpegImageFileName);
			}
		}
		finally {
			IoUtils.closeQuietly(false, os);
		}
	}

	/**
	 * Verify if the temporary file already exists. If yes, delete it.
	 *
	 * @param tempFile the temporary file.
	 */
	private static void verifyTempFile(@NonNull final File tempFile) {
		if (tempFile.exists()) {
			Log.w(Application.TAG, "tempFile " + tempFile.getName() + " already exists - deleting it");
			boolean success = FileUtil.deleteFile(tempFile);
			if (!success) {
				Log.w(Application.TAG, "Failed to delete file" + tempFile.getName());
			}
		}
	}

	/**
	 * Check if the settings allow a change of the JPEG.
	 *
	 * @return true if it is allowed to change image files.
	 */
	private static boolean changeJpegAllowed() {
		int storeOption = PreferenceUtil.getSharedPreferenceIntString(R.string.key_store_option, R.string.pref_default_store_options);
		return storeOption > 0;
	}

	/**
	 * Check if the settings allow a change of the EXIF data.
	 *
	 * @return true if it is allowed to change EXIF data.
	 */
	private static boolean changeExifAllowed() {
		int storeOption = PreferenceUtil.getSharedPreferenceIntString(R.string.key_store_option, R.string.pref_default_store_options);
		return storeOption == 2;
	}

	/**
	 * Exception indicating that an error appeared while storing EXIF data.
	 */
	public static final class ExifStorageException extends IOException {
		/**
		 * The serial version id.
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * Standard constructor, passing the causing exception.
		 *
		 * @param cause The exception.
		 */
		private ExifStorageException(final Throwable cause) {
			super(cause);
		}
	}

}
