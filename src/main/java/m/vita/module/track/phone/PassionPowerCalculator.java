/*
Copyright (C) 2011 The University of Michigan

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Please send inquiries to powertutor@umich.edu
 */

package m.vita.module.track.phone;

import android.content.Context;

import m.vita.module.track.comp.OLED;


/* Most of this file should be inheritted from DreamPowerCalculator as most of
 * the hardware model details will be the same modulo the coefficients.
 */
public class PassionPowerCalculator extends DreamPowerCalculator {
	public PassionPowerCalculator(Context context) {
		super(new PassionConstants(context));
	}

	public PassionPowerCalculator(PhoneConstants coeffs) {
		super(coeffs);
	}

	@Override
	public double getOledPower(OLED.OledData data) {
		if (!data.screenOn) {
			return 0;
		}
		if (data.pixPower == -1) {
			/* No pixel power available :(. */
			return coeffs.oledBasePower() + coeffs.lcdBrightness()
					* data.brightness;
		} else {
			return coeffs.oledBasePower() + data.pixPower * data.brightness;
		}
	}
}
