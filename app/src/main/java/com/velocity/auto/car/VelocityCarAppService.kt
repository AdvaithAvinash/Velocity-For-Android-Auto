package com.velocity.auto.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * The bind point the (patched) Android Auto host looks for. A normal car
 * app restricts this to Google's own signed host via [HostValidator] -
 * Velocity has no such signed host to check against, so it allows any
 * host to bind. That's the deliberate, necessary trade-off of running
 * outside Google's official allowlist at all.
 */
class VelocityCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = VelocitySession()
}
