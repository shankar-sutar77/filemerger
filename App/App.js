import { createSwitchNavigator, createDrawerNavigator,
        createStackNavigator } from 'react-navigation';

import {NetInfo} from 'react-native';
import LoginScreen from './components/Login'
import HomeScreen from './components/Home'
import SplitAndSend from './components/SplitAndSend'
import MergedItem from './components/MergedItem'
import ListOfFiles from './components/ListOfFiles'
import AuthLoadingScreen from './components/AuthLoading'

global.userName = null;
global.myip = '192.168.0.123:3001';

const Drawer = createDrawerNavigator({
   Home        : HomeScreen,
   SplitAndSend: SplitAndSend ,
   MergedItem  : MergedItem ,
   ListOfFiles : ListOfFiles ,
});

const AppStack = createStackNavigator({
  Home        : HomeScreen,
  SplitAndSend: SplitAndSend ,
  MergedItem  : MergedItem ,
  ListOfFiles : ListOfFiles ,
  Drawer      : Drawer
});
const AuthStack = createStackNavigator({ Login : LoginScreen });
console.disableYellowBox = true;
export default createSwitchNavigator(
  {
    AuthLoading : AuthLoadingScreen,
    App: AppStack,
    Auth: AuthStack,
  },
  {
    initialRouteName: 'AuthLoading',
  }
);
