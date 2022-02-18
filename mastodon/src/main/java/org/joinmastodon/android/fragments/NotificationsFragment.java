package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.notifications.GetNotifications;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.ReblogOrReplyLineStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;

public class NotificationsFragment extends BaseStatusListFragment<Notification>{
	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setTitle(R.string.notifications);
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(Notification n){
		ReblogOrReplyLineStatusDisplayItem titleItem=new ReblogOrReplyLineStatusDisplayItem(n.id, this, switch(n.type){
			case FOLLOW -> getString(R.string.user_followed_you, n.account.displayName);
			case FOLLOW_REQUEST -> getString(R.string.user_sent_follow_request, n.account.displayName);
			case MENTION -> getString(R.string.user_mentioned_you, n.account.displayName);
			case REBLOG -> getString(R.string.user_boosted, n.account.displayName);
			case FAVORITE -> getString(R.string.user_favorited, n.account.displayName);
			case POLL -> getString(R.string.poll_ended);
			case STATUS -> getString(R.string.user_posted, n.account.displayName);
		}, n.account.emojis, R.drawable.ic_fluent_arrow_reply_20_filled);
		if(n.status!=null){
			ArrayList<StatusDisplayItem> items=StatusDisplayItem.buildItems(this, n.status, accountID, n, knownAccounts);
			items.add(0, titleItem);
			return items;
		}else{
			return Collections.singletonList(titleItem);
		}
	}

	@Override
	protected void addAccountToKnown(Notification s){
		if(!knownAccounts.containsKey(s.account.id))
			knownAccounts.put(s.account.id, s.account);
		if(s.status!=null && !knownAccounts.containsKey(s.status.account.id))
			knownAccounts.put(s.status.account.id, s.status.account);
	}

	@Override
	protected void doLoadData(int offset, int count){
		new GetNotifications(offset>0 ? getMaxID() : null, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Notification> result){
						onDataLoaded(result, !result.isEmpty());
					}
				})
				.exec(accountID);
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
			loadData();
	}

	@Override
	public void onItemClick(String id){
		Notification n=getNotificationByID(id);
		if(n.status!=null){
			Status status=n.status;
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("status", Parcels.wrap(status));
			if(status.inReplyToAccountId!=null && knownAccounts.containsKey(status.inReplyToAccountId))
				args.putParcelable("inReplyToAccount", Parcels.wrap(knownAccounts.get(status.inReplyToAccountId)));
			Nav.go(getActivity(), ThreadFragment.class, args);
		}
	}

	@Override
	protected void updatePoll(String itemID, Poll poll){
		Notification notification=getNotificationByID(itemID);
		if(notification==null || notification.status==null)
			return;
		notification.status.poll=poll;
		super.updatePoll(itemID, poll);
	}

	private Notification getNotificationByID(String id){
		for(Notification n:data){
			if(n.id.equals(id))
				return n;
		}
		return null;
	}
}
