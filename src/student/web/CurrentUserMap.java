package student.web;


public class CurrentUserMap<T> extends SessionPersistenceMap<T> {
	private static final String USER = "app-user";
	public CurrentUserMap(Class<T> type) {
		super(type);
	}
	public T setCurrentUser(T user)
	{
		T currentUser = getUser();
		setUser(user);
		return currentUser;
	}
	public T getCurrentUser()
	{
		return getUser();
	}
	private T getUser()
	{
		return this.get(USER);
	}
	private void setUser(T user)
	{
		this.put(USER, user);
	}
	public void removeCurrentUser()
	{
		this.remove(USER);
	}
	public void logout()
	{
		this.remove(USER);
		this.clear();
	}

}
