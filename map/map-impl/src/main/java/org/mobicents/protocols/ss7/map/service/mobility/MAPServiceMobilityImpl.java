/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012.
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.protocols.ss7.map.service.mobility;

import org.apache.log4j.Logger;
import org.mobicents.protocols.asn.AsnInputStream;
import org.mobicents.protocols.asn.Tag;
import org.mobicents.protocols.ss7.map.MAPDialogImpl;
import org.mobicents.protocols.ss7.map.MAPProviderImpl;
import org.mobicents.protocols.ss7.map.MAPServiceBaseImpl;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPDialog;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPOperationCode;
import org.mobicents.protocols.ss7.map.api.MAPParsingComponentException;
import org.mobicents.protocols.ss7.map.api.MAPParsingComponentExceptionReason;
import org.mobicents.protocols.ss7.map.api.MAPServiceListener;
import org.mobicents.protocols.ss7.map.api.dialog.ServingCheckData;
import org.mobicents.protocols.ss7.map.api.dialog.ServingCheckResult;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPDialogMobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPServiceMobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPServiceMobilityListener;
import org.mobicents.protocols.ss7.map.dialog.ServingCheckDataImpl;
import org.mobicents.protocols.ss7.map.service.mobility.authentication.AuthenticationSetListImpl;
import org.mobicents.protocols.ss7.map.service.mobility.authentication.SendAuthenticationInfoRequestImpl;
import org.mobicents.protocols.ss7.map.service.mobility.authentication.SendAuthenticationInfoResponseImpl;
import org.mobicents.protocols.ss7.map.service.mobility.locationManagement.UpdateLocationRequestImpl;
import org.mobicents.protocols.ss7.map.service.mobility.locationManagement.UpdateLocationResponseImpl;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.Dialog;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.protocols.ss7.tcap.asn.TcapFactory;
import org.mobicents.protocols.ss7.tcap.asn.comp.ComponentType;
import org.mobicents.protocols.ss7.tcap.asn.comp.Invoke;
import org.mobicents.protocols.ss7.tcap.asn.comp.OperationCode;
import org.mobicents.protocols.ss7.tcap.asn.comp.Parameter;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class MAPServiceMobilityImpl extends MAPServiceBaseImpl implements MAPServiceMobility {

	protected Logger loger = Logger.getLogger(MAPServiceMobilityImpl.class);

	public MAPServiceMobilityImpl(MAPProviderImpl mapProviderImpl) {
		super(mapProviderImpl);
	}

	/*
	 * Creating a new outgoing MAP Mobility dialog and adding it to the
	 * MAPProvider.dialog collection
	 * 
	 */
	@Override
	public MAPDialogMobility createNewDialog(MAPApplicationContext appCntx, SccpAddress origAddress, AddressString origReference, SccpAddress destAddress,
			AddressString destReference) throws MAPException {

		// We cannot create a dialog if the service is not activated
		if (!this.isActivated())
			throw new MAPException(
					"Cannot create MAPDialogMobility because MAPServiceMobility is not activated");

		Dialog tcapDialog = this.createNewTCAPDialog(origAddress, destAddress);
		MAPDialogMobilityImpl dialog = new MAPDialogMobilityImpl(appCntx, tcapDialog, this.mapProviderImpl,
				this, origReference, destReference);

		this.putMAPDialogIntoCollection(dialog);

		return dialog;
	}

	@Override
	protected MAPDialogImpl createNewDialogIncoming(MAPApplicationContext appCntx, Dialog tcapDialog) {
		return new MAPDialogMobilityImpl(appCntx, tcapDialog, this.mapProviderImpl, this, null, null);
	}

	@Override
	public void addMAPServiceListener(MAPServiceMobilityListener mapServiceListener) {
		super.addMAPServiceListener(mapServiceListener);
	}

	@Override
	public void removeMAPServiceListener(MAPServiceMobilityListener mapServiceListener) {
		super.removeMAPServiceListener(mapServiceListener);
	}

	@Override
	public ServingCheckData isServingService(MAPApplicationContext dialogApplicationContext) {
		MAPApplicationContextName ctx = dialogApplicationContext.getApplicationContextName();
		int vers = dialogApplicationContext.getApplicationContextVersion().getVersion();

		switch (ctx) {
		case infoRetrievalContext:
			if (vers >= 1 && vers <= 3) {
				return new ServingCheckDataImpl(ServingCheckResult.AC_Serving);
			} else if (vers > 3) {
				long[] altOid = dialogApplicationContext.getOID();
				altOid[7] = 3;
				ApplicationContextName alt = TcapFactory.createApplicationContextName(altOid);
				return new ServingCheckDataImpl(ServingCheckResult.AC_VersionIncorrect, alt);
			} else {
				return new ServingCheckDataImpl(ServingCheckResult.AC_VersionIncorrect);
			}

		case networkLocUpContext:
			if (vers >= 1 && vers <= 3) {
				return new ServingCheckDataImpl(ServingCheckResult.AC_Serving);
			} else if (vers > 3) {
				long[] altOid = dialogApplicationContext.getOID();
				altOid[7] = 3;
				ApplicationContextName alt = TcapFactory.createApplicationContextName(altOid);
				return new ServingCheckDataImpl(ServingCheckResult.AC_VersionIncorrect, alt);
			} else {
				return new ServingCheckDataImpl(ServingCheckResult.AC_VersionIncorrect);
			}

		case equipmentMngtContext:
			if (vers >= 1 && vers <= 3) {
				return new ServingCheckDataImpl(ServingCheckResult.AC_Serving);
			} else if (vers > 3) {
				long[] altOid = dialogApplicationContext.getOID();
				altOid[7] = 3;
				ApplicationContextName alt = TcapFactory.createApplicationContextName(altOid);
				return new ServingCheckDataImpl(ServingCheckResult.AC_VersionIncorrect, alt);
			} else {
				return new ServingCheckDataImpl(ServingCheckResult.AC_VersionIncorrect);
			}

		case anyTimeEnquiryContext:
			if (vers >= 3 && vers <= 3) {
				return new ServingCheckDataImpl(ServingCheckResult.AC_Serving);
			} else if (vers > 3) {
				long[] altOid = dialogApplicationContext.getOID();
				altOid[7] = 3;
				ApplicationContextName alt = TcapFactory.createApplicationContextName(altOid);
				return new ServingCheckDataImpl(ServingCheckResult.AC_VersionIncorrect, alt);
			} else {
				return new ServingCheckDataImpl(ServingCheckResult.AC_VersionIncorrect);
			}
		}
		
		return new ServingCheckDataImpl(ServingCheckResult.AC_NotServing);
	}

	@Override
	public MAPApplicationContext getMAPv1ApplicationContext(int operationCode, Invoke invoke) {

		switch (operationCode) {
		case MAPOperationCode.updateLocation:
			return MAPApplicationContext.getInstance(MAPApplicationContextName.networkLocUpContext, MAPApplicationContextVersion.version1);
		case MAPOperationCode.SendParameters:
			return MAPApplicationContext.getInstance(MAPApplicationContextName.infoRetrievalContext, MAPApplicationContextVersion.version1);
		case MAPOperationCode.checkIMEI:
			return MAPApplicationContext.getInstance(MAPApplicationContextName.equipmentMngtContext, MAPApplicationContextVersion.version1);
		}

		return null;
	}

	@Override
	public void processComponent(ComponentType compType, OperationCode oc, Parameter parameter, MAPDialog mapDialog, Long invokeId, Long linkedId)
			throws MAPParsingComponentException {

		// if an application-context-name different from version 1 is
		// received in a syntactically correct TC-
		// BEGIN indication primitive but is not acceptable from a load
		// control point of view, the MAP PM
		// shall ignore this dialogue request. The MAP-user is not informed.
		if (compType == ComponentType.Invoke && this.mapProviderImpl.isCongested()) {
			// we agree mobility services when congestion
		}

		MAPDialogMobilityImpl mapDialogMobilityImpl = (MAPDialogMobilityImpl) mapDialog;

		Long ocValue = oc.getLocalOperationCode();
		if (ocValue == null)
			new MAPParsingComponentException("", MAPParsingComponentExceptionReason.UnrecognizedOperation);
		MAPApplicationContextName acn = mapDialog.getApplicationContext().getApplicationContextName();
		int vers = mapDialog.getApplicationContext().getApplicationContextVersion().getVersion();
		int ocValueInt = (int) (long)ocValue;

		switch (ocValueInt) {
		case MAPOperationCode.sendAuthenticationInfo:
			if (acn == MAPApplicationContextName.infoRetrievalContext && vers >= 2) {
				if (compType == ComponentType.Invoke)
					this.sendAuthenticationInfoRequest(parameter, mapDialogMobilityImpl, invokeId);
				else
					this.sendAuthenticationInfoResponse(parameter, mapDialogMobilityImpl, invokeId);
			}
			break;

		case MAPOperationCode.updateLocation:
			if (acn == MAPApplicationContextName.networkLocUpContext ) {
				if (compType == ComponentType.Invoke)
					this.updateLocationRequest(parameter, mapDialogMobilityImpl, invokeId);
				else
					this.updateLocationResponse(parameter, mapDialogMobilityImpl, invokeId);
			}
			break;
			
		default:
			new MAPParsingComponentException("", MAPParsingComponentExceptionReason.UnrecognizedOperation);
		}
	}

	private void sendAuthenticationInfoRequest(Parameter parameter, MAPDialogMobilityImpl mapDialogImpl, Long invokeId) throws MAPParsingComponentException {

		long version = mapDialogImpl.getApplicationContext().getApplicationContextVersion().getVersion();
		SendAuthenticationInfoRequestImpl ind = new SendAuthenticationInfoRequestImpl(version);
		if (version >= 3) {
			if (parameter != null) {
				if (parameter.getTag() != Tag.SEQUENCE || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || parameter.isPrimitive())
					throw new MAPParsingComponentException(
							"Error while decoding sendAuthenticationInfoRequest V3: Bad tag or tagClass or parameter is primitive, received tag="
									+ parameter.getTag(), MAPParsingComponentExceptionReason.MistypedParameter);

				byte[] buf = parameter.getData();
				AsnInputStream ais = new AsnInputStream(buf);
				ind.decodeData(ais, buf.length);
			}
		} else {
			if (parameter == null)
				throw new MAPParsingComponentException("Error while decoding sendAuthenticationInfoRequest V2: Parameter is mandatory but not found",
						MAPParsingComponentExceptionReason.MistypedParameter);

			if (parameter.getTag() != Tag.STRING_OCTET || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || !parameter.isPrimitive())
				throw new MAPParsingComponentException(
						"Error while decoding sendAuthenticationInfoRequest V2: Bad tag or tagClass or parameter is not primitive, received tag="
								+ parameter.getTag(), MAPParsingComponentExceptionReason.MistypedParameter);

			byte[] buf = parameter.getData();
			AsnInputStream ais = new AsnInputStream(buf);
			ind.decodeData(ais, buf.length);
		}

		ind.setInvokeId(invokeId);
		ind.setMAPDialog(mapDialogImpl);

		for (MAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onMAPMessage(ind);
				((MAPServiceMobilityListener) serLis).onSendAuthenticationInfoRequest(ind);
			} catch (Exception e) {
				loger.error("Error processing sendAuthenticationInfoRequest: " + e.getMessage(), e);
			}
		}
	}

	private void sendAuthenticationInfoResponse(Parameter parameter, MAPDialogMobilityImpl mapDialogImpl, Long invokeId) throws MAPParsingComponentException {

		long version = mapDialogImpl.getApplicationContext().getApplicationContextVersion().getVersion();
		SendAuthenticationInfoResponseImpl ind = new SendAuthenticationInfoResponseImpl(version);
		if (version >= 3) {
			if (parameter != null) {
				if (parameter.getTag() != SendAuthenticationInfoResponseImpl._TAG_General || parameter.getTagClass() != Tag.CLASS_CONTEXT_SPECIFIC || parameter.isPrimitive())
					throw new MAPParsingComponentException(
							"Error while decoding sendAuthenticationInfoResponse: Bad tag or tagClass or parameter is primitive, received tag="
									+ parameter.getTag(), MAPParsingComponentExceptionReason.MistypedParameter);

				byte[] buf = parameter.getData();
				AsnInputStream ais = new AsnInputStream(buf);
				ind.decodeData(ais, buf.length);
			}
		} else {
			if (parameter != null) {
				if (parameter.getTag() != AuthenticationSetListImpl._TAG_tripletList || parameter.getTag() != AuthenticationSetListImpl._TAG_quintupletList
						|| parameter.getTagClass() != Tag.CLASS_CONTEXT_SPECIFIC || parameter.isPrimitive())
					throw new MAPParsingComponentException(
							"Error while decoding sendAuthenticationInfoResponse: Bad tag or tagClass or parameter is primitive, received tag="
									+ parameter.getTag(), MAPParsingComponentExceptionReason.MistypedParameter);

				byte[] buf = parameter.getData();
				AsnInputStream ais = new AsnInputStream(buf);
				ind.decodeData(ais, buf.length);
			}
		}

		ind.setInvokeId(invokeId);
		ind.setMAPDialog(mapDialogImpl);

		for (MAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onMAPMessage(ind);
				((MAPServiceMobilityListener) serLis).onSendAuthenticationInfoResponse(ind);
			} catch (Exception e) {
				loger.error("Error processing sendAuthenticationInfoResponse: " + e.getMessage(), e);
			}
		}
	}

	private void updateLocationRequest(Parameter parameter, MAPDialogMobilityImpl mapDialogImpl, Long invokeId) throws MAPParsingComponentException {
		
		if (parameter == null)
			throw new MAPParsingComponentException("Error while decoding updateLocationRequest: Parameter is mandatory but not found",
					MAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.SEQUENCE || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || parameter.isPrimitive())
			throw new MAPParsingComponentException("Error while decoding updateLocationRequest: Bad tag or tagClass or parameter is primitive, received tag="
					+ parameter.getTag(), MAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		UpdateLocationRequestImpl ind = new UpdateLocationRequestImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setMAPDialog(mapDialogImpl);

		for (MAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onMAPMessage(ind);
				((MAPServiceMobilityListener) serLis).onUpdateLocationRequest(ind);
			} catch (Exception e) {
				loger.error("Error processing updateLocationRequest: " + e.getMessage(), e);
			}
		}
	}

	private void updateLocationResponse(Parameter parameter, MAPDialogMobilityImpl mapDialogImpl, Long invokeId) throws MAPParsingComponentException {
		
		if (parameter == null)
			throw new MAPParsingComponentException("Error while decoding updateLocationResponse: Parameter is mandatory but not found",
					MAPParsingComponentExceptionReason.MistypedParameter);

		if (parameter.getTag() != Tag.SEQUENCE || parameter.getTagClass() != Tag.CLASS_UNIVERSAL || parameter.isPrimitive())
			throw new MAPParsingComponentException("Error while decoding updateLocationResponse: Bad tag or tagClass or parameter is primitive, received tag="
					+ parameter.getTag(), MAPParsingComponentExceptionReason.MistypedParameter);

		byte[] buf = parameter.getData();
		AsnInputStream ais = new AsnInputStream(buf);
		UpdateLocationResponseImpl ind = new UpdateLocationResponseImpl();
		ind.decodeData(ais, buf.length);

		ind.setInvokeId(invokeId);
		ind.setMAPDialog(mapDialogImpl);

		for (MAPServiceListener serLis : this.serviceListeners) {
			try {
				serLis.onMAPMessage(ind);
				((MAPServiceMobilityListener) serLis).onUpdateLocationResponse(ind);
			} catch (Exception e) {
				loger.error("Error processing updateLocationResponse: " + e.getMessage(), e);
			}
		}
	}

}
