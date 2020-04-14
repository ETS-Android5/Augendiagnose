package de.jeisfeld.augendiagnoselib.fragments;

import android.app.Fragment;
import android.graphics.Color;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jeisfeld.augendiagnoselib.R;
import de.jeisfeld.augendiagnoselib.util.DialogUtil;
import de.jeisfeld.augendiagnoselib.util.imagefile.EyePhoto;
import de.jeisfeld.augendiagnoselib.util.imagefile.EyePhotoPair;
import de.jeisfeld.augendiagnoselib.util.imagefile.ImageUtil;

/**
 * Base listFoldersFragment to display the pictures in an eye photo folder (in pairs) Abstract class - child classes
 * determine the detailed actions.
 */
public abstract class ListPicturesForNameBaseFragment extends Fragment {
	/**
	 * The resource key of the name.
	 */
	private static final String STRING_NAME = "de.jeisfeld.augendiagnoselib.NAME";
	/**
	 * The resource key of the parent folder.
	 */
	private static final String STRING_PARENTFOLDER = "de.jeisfeld.augendiagnoselib.PARENTFOLDER";

	/**
	 * The parent folder.
	 */
	@Nullable
	private String mParentFolder;
	/**
	 * The name for which the eye photos should be displayed.
	 */
	@Nullable
	private String mName;

	/**
	 * The list view showing the pictures.
	 */
	private ListView mListView;

	/**
	 * The array of eye photo pairs.
	 */
	private EyePhotoPair[] mEyePhotoPairs;

	/**
	 * Initialize the listFoldersFragment with parentFolder and name.
	 *
	 * @param initialParentFolder The parent folder
	 * @param initialName the name
	 */
	public final void setParameters(final String initialParentFolder, final String initialName) {
		Bundle args = new Bundle();
		args.putString(STRING_PARENTFOLDER, initialParentFolder);
		args.putString(STRING_NAME, initialName);

		setArguments(args);
	}

	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();

		mName = args.getString(STRING_NAME);
		mParentFolder = args.getString(STRING_PARENTFOLDER);
	}

	// OVERRIDABLE
	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (getView() == null) {
			return;
		}

		TextView headerNameView = getView().findViewById(R.id.textTitleName);
		headerNameView.setText(mName);

		createAndStoreEyePhotoList();

		mListView = getView().findViewById(R.id.listViewForName);

		// prevent highlighting
		mListView.setCacheColorHint(Color.TRANSPARENT);
		mListView.setSelector(new StateListDrawable());
	}

	/**
	 * Create the list of eye photo pairs for display. Photos are arranged in pairs (right-left) by date.
	 *
	 * @param folder the folder where the photos are located.
	 * @return The list of eye photo pairs.
	 */
	@NonNull
	private EyePhotoPair[] createEyePhotoList(@NonNull final File folder) {
		Map<Date, EyePhotoPair> eyePhotoMap = new TreeMap<>(new Comparator<Date>() {
			@Override
			public int compare(@NonNull final Date lhs, @NonNull final Date rhs) {
				return rhs.compareTo(lhs);
			}
		});

		File[] files = folder.listFiles(new ImageUtil.ImageFileFilter());

		if (files == null) {
			return new EyePhotoPair[0];
		}

		for (File f : files) {
			EyePhoto eyePhoto = new EyePhoto(f);

			if (eyePhoto.isFormatted()) {
				Date date = eyePhoto.getDate();

				if (eyePhotoMap.containsKey(date)) {
					EyePhotoPair eyePhotoPair = eyePhotoMap.get(date);
					eyePhotoPair.setEyePhoto(eyePhoto);
				}
				else {
					EyePhotoPair eyePhotoPair = new EyePhotoPair();
					eyePhotoPair.setEyePhoto(eyePhoto);
					eyePhotoMap.put(date, eyePhotoPair);
				}
			}
			else {
				DialogUtil.displayError(getActivity(), R.string.message_dialog_unformatted_file, false,
						f.getAbsolutePath());
			}
		}

		return eyePhotoMap.values().toArray(new EyePhotoPair[0]);
	}

	/**
	 * Create the list of eye photo pairs and store them.
	 *
	 * @return true if there are still eye photos remaining.
	 */
	protected final boolean createAndStoreEyePhotoList() {
		mEyePhotoPairs = createEyePhotoList(new File(mParentFolder, mName));

		getActivity().findViewById(R.id.textViewNoImagesForName).setVisibility(mEyePhotoPairs.length == 0 ? View.VISIBLE : View.GONE);

		return mEyePhotoPairs.length > 0;
	}

	/**
	 * Get the parent folder.
	 *
	 * @return The parent folder.
	 */
	@Nullable
	public final String getParentFolder() {
		return mParentFolder;
	}

	/**
	 * Get the name.
	 *
	 * @return The name.
	 */
	@Nullable
	public final String getName() {
		return mName;
	}

	protected final ListView getListView() {
		return mListView;
	}

	protected final EyePhotoPair[] getEyePhotoPairs() {
		return mEyePhotoPairs;
	}

}
