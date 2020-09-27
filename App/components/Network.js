import { NetInfo, ToastAndroid } from 'react-native'

export default class Network {

  constructor() {
    NetInfo.isConnected.addEventListener(
      'connectionChange',
      this.handleFirstConnectivityChange
    );
  }

  handleFirstConnectivityChange = (isConnected) => {
    console.log('Then, is ' + (isConnected ? 'online connected..!' : 'offline connect to network..!'));
    if(isConnected)
      ToastAndroid.show('online connected..!', ToastAndroid.SHORT)
    else
      ToastAndroid.show('offline connect to network..!',ToastAndroid.LONG);
  };
}
