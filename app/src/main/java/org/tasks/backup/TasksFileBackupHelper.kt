package org.tasks.backup

import android.app.backup.BackupDataInputStream
import android.app.backup.BackupDataOutput
import android.app.backup.FileBackupHelper
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.todoroo.astrid.backup.BackupConstants
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ApplicationComponent
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.preferences.Preferences
import timber.log.Timber
import java.io.File

class TasksFileBackupHelper(
        private val context: Context
) : FileBackupHelper(context, BackupConstants.INTERNAL_BACKUP) {

    @EntryPoint
    @InstallIn(ApplicationComponent::class)
    internal interface TasksFileBackupHelperEntryPoint {
        val tasksJsonImporter: TasksJsonImporter
        val preferences: Preferences
    }

    override fun performBackup(
            oldState: ParcelFileDescriptor?,
            data: BackupDataOutput?,
            newState: ParcelFileDescriptor?
    ) {
        val preferences = hilt.preferences
        if (!preferences.androidBackupServiceEnabled()) {
            Timber.d("Android backup service disabled")
            return
        }
        file
                ?.let {
                    Timber.d("Backing up ${it.absolutePath}")
                    super.performBackup(oldState, data, newState)
                    preferences.setLong(R.string.p_backups_android_backup_last, it.lastModified())
                }
                ?: Timber.e("$path not found")
    }

    private val path: String
        get() = "${context.filesDir.absolutePath}/${BackupConstants.INTERNAL_BACKUP}"

    private val file: File?
        get() = File(path).takeIf { it.exists() }

    override fun restoreEntity(data: BackupDataInputStream?) {
        super.restoreEntity(data)

        file
                ?.let {
                    runBlocking {
                        Timber.d("Restoring ${it.absolutePath}")
                        hilt.tasksJsonImporter.importTasks(context, Uri.fromFile(it), null)
                    }
                }
                ?: Timber.e("$path not found")
    }

    private val hilt: TasksFileBackupHelperEntryPoint
        get() = EntryPointAccessors
                .fromApplication(context, TasksFileBackupHelperEntryPoint::class.java)
}