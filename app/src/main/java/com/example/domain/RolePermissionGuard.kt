package com.example.domain

import com.example.models.CropScanRecord
import com.example.models.Farm
import com.example.models.TelemetrySource
import com.example.models.UserProfile
import com.example.models.UserRole

object RolePermissionGuard {

    fun canCreateFarm(user: UserProfile?): Boolean =
        user != null && (user.resolvedRole() == UserRole.FARMER || user.resolvedRole() == UserRole.ADMIN)

    fun canAccessFarm(user: UserProfile?, farm: Farm): Boolean {
        if (user == null) return false
        return when (user.resolvedRole()) {
            UserRole.ADMIN -> true
            UserRole.FARMER -> farm.ownerId == user.userId
            UserRole.EXPERT -> user.userId in farm.sharedExpertIds
        }
    }

    fun canEditFarm(user: UserProfile?, farm: Farm): Boolean {
        if (user == null) return false
        return user.resolvedRole() == UserRole.ADMIN ||
            (user.resolvedRole() == UserRole.FARMER && farm.ownerId == user.userId)
    }

    fun canVerifyScan(user: UserProfile?, scan: CropScanRecord): Boolean {
        if (user == null) return false
        return user.resolvedRole() == UserRole.ADMIN ||
            (user.resolvedRole() == UserRole.EXPERT && user.userId in scan.sharedExpertIds)
    }

    fun canAccessAdminManagement(user: UserProfile?): Boolean =
        user?.resolvedRole() == UserRole.ADMIN

    fun canClientSubmitTelemetrySource(source: TelemetrySource): Boolean =
        source == TelemetrySource.MANUAL_LOG
}
