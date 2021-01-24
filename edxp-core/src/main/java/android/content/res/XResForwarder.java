package android.content.res;

/**
 * Instances of this class can be used for {@link XResources#setReplacement(String, String, String, Object)}
 * and its variants. They forward the resource request to a different {@link android.content.res.Resources}
 * instance with a possibly different ID.
 *
 * <p>Usually, instances aren't created directly but via {@link XModuleResources#fwd}.
 */
public class XResForwarder {
	private final Resources res;
	private final int id;

	/**
	 * Creates a new instance.
	 *
	 * @param res The target {@link android.content.res.Resources} instance to forward requests to.
	 * @param id The target resource ID.
	 */
	public XResForwarder(Resources res, int id) {
		this.res = res;
		this.id = id;
	}

	/** Returns the target {@link android.content.res.Resources} instance. */
	public Resources getResources() {
		return res;
	}

	/** Returns the target resource ID. */
	public int getId() {
		return id;
	}
}
