/*
 * Copyright (C) 2014 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.anhonesteffort.flock.sync;

import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.os.RemoteException;
import android.util.Log;

import com.google.common.base.Optional;

import org.anhonesteffort.flock.crypto.InvalidMacException;
import org.anhonesteffort.flock.webdav.InvalidComponentException;
import org.anhonesteffort.flock.webdav.PropertyParseException;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLException;

/**
 * rhodey
 */
public class SyncWorkerUtil {

  private static final String TAG = "org.anhonesteffort.flock.sync.SyncUtil";

  protected static final int MAX_COMPONENTS_PER_REPORT = 50;

  // TODO: need to handle RegistrationApiExceptions
  public static void handleException(Context context, Exception e, SyncResult result) {
    if (e instanceof DavException) {
      DavException ex = (DavException) e;
      Log.e(TAG, "error code: " + ex.getErrorCode() + ", status phrase: " + ex.getStatusPhrase(), e);

      if (ex.getErrorCode() == DavServletResponse.SC_UNAUTHORIZED)
        result.stats.numAuthExceptions++;
      else if (ex.getErrorCode() == OwsWebDav.STATUS_PAYMENT_REQUIRED)
        result.stats.numSkippedEntries++;
      else if (ex.getErrorCode() != DavServletResponse.SC_PRECONDITION_FAILED)
        result.stats.numConflictDetectedExceptions++;
    }

    else if (e instanceof InvalidComponentException) {
      InvalidComponentException ex = (InvalidComponentException) e;
      result.stats.numParseExceptions++;
      Log.e(TAG, ex.toString(), ex);
    }

    // server is giving us funky stuff...
    else if (e instanceof PropertyParseException) {
      PropertyParseException ex = (PropertyParseException) e;
      result.stats.numParseExceptions++;
      Log.e(TAG, ex.toString(), ex);
    }

    // client is doing funky stuff...
    else if (e instanceof RemoteException || e instanceof OperationApplicationException) {
      result.stats.numParseExceptions++;
      Log.e(TAG, e.toString(), e);
    }

    else if (e instanceof InvalidMacException) {
      Log.e(TAG, "BAD MAC IN SYNC!!! 0.o ", e);
      result.stats.numParseExceptions++;
    }
    else if (e instanceof GeneralSecurityException) {
      Log.e(TAG, "crypto problems in sync 0.u ", e);
      result.stats.numParseExceptions++;
    }

    else if (e instanceof SSLException) {
      Log.e(TAG, "SSL PROBLEM IN SYNC!!! 0.o ", e);
      result.stats.numIoExceptions++;
    }
    else if (e instanceof IOException) {
      Log.e(TAG, "who knows...", e);
      result.stats.numIoExceptions++;
    }

    else if (!(e instanceof InterruptedException)) {
      result.stats.numIoExceptions++;
      Log.e(TAG, "DID NOT CATCH THIS EXCEPTION CORRECTLY!!! >> " + e.toString(), e);
    }
  }

  protected static void handleMakeFlockCollection(AbstractLocalComponentCollection<?> localCollection,
                                                  HidingDavCollection<?>              remoteCollection)
      throws PropertyParseException, DavException,
             RemoteException, GeneralSecurityException, IOException
  {
    if (!remoteCollection.isFlockCollection()) {
      if (!localCollection.getDisplayName().isPresent())
        remoteCollection.makeFlockCollection(" ");
      else
        remoteCollection.makeFlockCollection(localCollection.getDisplayName().get());
    }
  }

  protected static void handleRefreshCollectionProperties(Context                context,
                                                          SyncResult             result,
                                                          HidingDavCollection<?> remoteCollection)
  {
    try {

      remoteCollection.fetchProperties();

    }  catch (DavException e) {
      SyncWorkerUtil.handleException(context, e, result);
    } catch (IOException e) {
      SyncWorkerUtil.handleException(context, e, result);
    }
  }

  // TODO: should we reset the UUID and keep trying?
  protected static void handleServerRejectedLocalComponent(AbstractLocalComponentCollection<?> localCollection,
                                                           Long                                localId,
                                                           Context                             context,
                                                           SyncResult                          result)
  {
    Log.e(TAG, "handleServerRejectedLocalComponent() >> " + localId);

    try {

      localCollection.removeComponent(localId);
      localCollection.commitPendingOperations();

    } catch (RemoteException e) {
      SyncWorkerUtil.handleException(context, e, result);
    } catch (OperationApplicationException e) {
      SyncWorkerUtil.handleException(context, e, result);
    }
  }

  protected static void handleServerErrorOnPushNewLocalComponent(AbstractLocalComponentCollection<?> localCollection,
                                                                 Long                                localId,
                                                                 Context                             context,
                                                                 SyncResult                          result)
  {
    Log.e(TAG, "handleServerErrorOnPushNewLocalComponent() >> " + localId);

    try {

      localCollection.setUidToNull(localId);
      localCollection.commitPendingOperations();

    } catch (RemoteException e) {
      SyncWorkerUtil.handleException(context, e, result);
    } catch (OperationApplicationException e) {
      SyncWorkerUtil.handleException(context, e, result);
    }
  }

  protected static List<List<String>> handlePartitionUidsForReports(List<String> uids) {
    LinkedList<List<String>> partitionedLists = new LinkedList<List<String>>();
    List<String>             nextList         = new LinkedList<String>();

    for (int i = 0; i < uids.size(); i++) {
      if (i == 0)
        partitionedLists.add(nextList);
      else if (i >= MAX_COMPONENTS_PER_REPORT && (i % MAX_COMPONENTS_PER_REPORT) == 0) {
        nextList = new LinkedList<String>();
        partitionedLists.add(nextList);
      }

      nextList.add(uids.get(i));
    }

    return partitionedLists;
  }

  protected static List<List<String>> handlePartitionUidsForReports(Set<String> uidSet) {
    List<String> uidList = new LinkedList<String>();
    for (String uid : uidSet)
      uidList.add(uid);

    return handlePartitionUidsForReports(uidList);
  }

  protected static List<String> handleFilterUidsMissingLocally(AbstractLocalComponentCollection<?> localCollection,
                                                               Set<String>                         uids)
      throws RemoteException
  {
    List<String> uidsMissingLocally = new LinkedList<String>();

    for (String uid : uids) {
      Optional<Long> localId = localCollection.getLocalIdForUid(uid);
      if (!localId.isPresent())
        uidsMissingLocally.add(uid);
    }

    return uidsMissingLocally;
  }

  protected static <T> HashMap<String, Optional<String>> handleFilterUidsChangedRemotely(AbstractLocalComponentCollection<T> localCollection,
                                                                                         HashMap<String, String>             remoteUidETagMap)
      throws RemoteException
  {
    HashMap<String, Optional<String>> localUidETagMap = new HashMap<String, Optional<String>>();

    for (java.util.Map.Entry<String, String> remoteETagEntry : remoteUidETagMap.entrySet()) {
      Optional<String> localETag = localCollection.getETagForUid(remoteETagEntry.getKey());
      if (!localETag.isPresent() || !localETag.get().equals(remoteETagEntry.getValue()))
        localUidETagMap.put(remoteETagEntry.getKey(), localETag);
    }

    return localUidETagMap;
  }

  /*
  TODO:
    If we're getting invalid component and mac exceptions here they will likely continue to show
    up unless we, like, delete them. so eventually these should be put into some queue and then
    run through and removed from the remote server.

    for now we will just log them and hope the logs somehow come our way, lame :( :( :(
   */
  protected static void handleDoStuffWithMultiStatusResult(List<String>                  uidsRequested,
                                                           DecryptedMultiStatusResult<?> multiStatusResult,
                                                           Context                       context,
                                                           SyncResult                    syncResult)
  {
    if (uidsRequested.size() != multiStatusResult.getComponentETagPairs().size()) {
      Log.w(TAG, "requested " + uidsRequested.size() + " components *BUT INSTEAD* received " +
                 multiStatusResult.getComponentETagPairs().size());
    }

    for (InvalidRemoteComponentException e : multiStatusResult.getInvalidComponentExceptions())
      SyncWorkerUtil.handleException(context, e, syncResult);

    for (InvalidMacException e : multiStatusResult.getInvalidMacExceptions())
      SyncWorkerUtil.handleException(context, e, syncResult);
  }
}
