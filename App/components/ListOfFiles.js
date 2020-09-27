import React, { Component } from 'react';
import {
  Platform, StyleSheet, Text, View, Image,
  ActivityIndicator, TextInput, Linking, Alert,
  ScrollView, RefreshControl, ToastAndroid, Modal,
  Dimensions, NetInfo
} from 'react-native';
import { Button, Icon } from 'react-native-elements'
import VideoPlayer from 'react-native-video-player';

export default class ListOfFiles extends Component {

  static navigationOptions = ({ navigation }) => {
    return {
      title: 'List Of Files',
      headerRight: (<Button title={global.userName.toUpperCase()}
        backgroundColor="green"
        buttonStyle={{ height: 10, marginRight: -15 }}
        fontWeight="bold"
        iconRight={{ name: 'user', type: 'evilicon', size: 25 }}
        onPress={this.profile} />)
    };
  };


  constructor(props) {
    super(props);
    this.state = {
      listoffiles: [],
      mediaID: 0,
      refreshing: false,
      modalVisible: false,
      mediaURI: '',
      mediaType: '',
      loading: false,
      windowWidth: 0,
      windowHeight: 0
    }
  };

  showDialog = (media) => {
    this.setState({
      mediaID: media.id
    });
    Alert.alert(
      'Delete',
      'Do you want to delete this media \n' + media.filename,
      [
        { text: 'NO' },
        { text: 'YES', onPress: () => this.deleteMedia(this.state.mediaID) },
      ],
      { cancelable: true }
    );
  };

  handleCancel = () => {
    this.setState({ dialogVisible: false });
  };

  handleDelete = () => {
    this.setState({ dialogVisible: false });
    this.deleteMedia(this.state.mediaID);
  };

  _onRefresh = () => {
    this.setState({ refreshing: true });
    this.getFilesList();
  };

  getFilesList = () => {

    NetInfo.isConnected.fetch().then(isConnected => {
      if (isConnected == false) {
        ToastAndroid.show('offline connect to network..!', 700);
      } else {
        const { navigation } = this.props;
        let webSocket = new WebSocket('ws://' + myip);
        webSocket.onopen = () => {
          let request = {
            action: 'getFiles',
            username: global.userName.toLowerCase()
          };
          webSocket.send(JSON.stringify(request));
        };

        webSocket.onmessage = (event) => {
          let response = JSON.parse(event.data);
          this.setState({
            listoffiles: response,
            refreshing: false
          });
          ToastAndroid.show('List Updated..!', 700);
        }
      }
    });
  };

  componentWillMount = () => {
    let window = Dimensions.get('screen');
    console.log("WINDOW Dimensions---->", window);
    this.setState({
      windowWidth: window.width,
      windowHeight: window.height
    })
    this.getFilesList();
    global.getFiles = setInterval(this.getFilesList, 30000);
  };

  componentWillUnmount = () => {
    clearInterval(global.getFiles);
  }

  deleteMedia = (mediaID) => {
    const { navigation } = this.props;
    let webSocket = new WebSocket('ws://' + myip);
    webSocket.onopen = () => {
      let request = {
        action: 'deleteMedia',
        username: global.userName,
        mediaID: mediaID
      };
      webSocket.send(JSON.stringify(request));
    }

    webSocket.onmessage = (event) => {
      let response = JSON.parse(event.data);
      if (response == true) {
        this.getFilesList();
      } else {
        alert("Media delete Unsuccessful...!");
      }
    }
  };

  showMedia = (mediaInfo) => {
    NetInfo.isConnected.fetch().then(isConnected => {
      if (isConnected == false) {
        ToastAndroid.show('offline connect to network..!', 700);
      } else {
        this.setState({ modalVisible: true });
        let AWS = require('aws-sdk');
        let params = { Bucket: 'ssssssss', Key: 'sssssss' + mediaInfo.filename };
        let s3 = new AWS.S3({
          accessKeyId: 'ssssssssssssss',
          secretAccessKey: 'ssssssssssss', region: 'us-east-1'
        });
        s3.getSignedUrl('getObject', params, (err, url) => {
          console.log('Your generated pre-signed URL is', url);
          this.setState({ mediaURI: url, mediaType: mediaInfo.content_type });
        });
      }
    });
  }

    render() {

      if (this.state.listoffiles.length == 0) {
        return (
          <View style={styles.container}><Text>No files to show</Text></View>
        );
      }
      return (
        <ScrollView style={{ backgroundColor: 'white' }}
          refreshControl={
            <RefreshControl
              onRefresh={this._onRefresh}
              refreshing={this.state.refreshing}
            />
          }>
          <View style={styles.container}>

            <Modal
              animationType="slide"
              transparent={true}
              onRequestClose={() => { this.setState({ modalVisible: false }) }}
              visible={this.state.modalVisible}>
              <View style={styles.modalContainer}>
                {(this.state.loading) ?
                  <ActivityIndicator
                    size="large" style={styles.activityIndicator}
                    color="blue" /> : null}
                {
                  (this.state.mediaType.includes('video')) ?

                    <VideoPlayer defaultMuted
                      video={{ uri: this.state.mediaURI }}
                      pauseOnPress disableControlsAutoHide
                      onLoadStart={() => { this.setState({ loading: true }); console.log("onLoadStart") }}
                      onLoad={() => { this.setState({ loading: false }); console.log("OnLoad") }}
                      videoWidth={this.state.windowWidth}
                      videoHeight={this.state.windowHeight}
                    />
                    :
                    <Image
                      onLoadStart={() => { this.setState({ loading: true }) }}
                      onLoadEnd={() => { this.setState({ loading: false }) }}
                      style={styles.imageStyle}
                      source={{ uri: this.state.mediaURI }}
                    />
                }
                <View style={styles.closeButton}>
                  <Button rounded
                    title='close'
                    buttonStyle={styles.deleteButton}
                    onPress={() => { this.setState({ modalVisible: false }) }}
                  />
                </View>
              </View>
            </Modal>

            <View style={styles.titleRow}>
              <View style={styles.elements}>
                <Text style={styles.title}>Type</Text>
              </View>
              <View style={{ width: 70, alignItems: 'center' }}>
                <Text style={styles.title}>From</Text>
              </View>
              <View style={styles.filename}>
                <Text style={styles.title}>FileName</Text>
              </View>
              <View style={styles.elements}>
                <Text style={styles.title}>View</Text>
              </View>
              <View style={styles.elements}>
                <Text style={styles.title}>Delete</Text>
              </View>
            </View>
            {
              Object.values(this.state.listoffiles).map((values) => {
                return (
                  <View style={styles.row}>
                    <View style={styles.elements}>
                      {
                        (values.content_type.includes('video')) ?
                          <Icon name='play-video' color='green' size={30}
                            type='foundation' /> :
                          <Icon name='image' type='font-awesome'
                            color='green' />
                      }
                    </View>
                    <View style={{ width: 70, alignItems: 'center' }}>
                      <Text>{values.sender}</Text>
                    </View>
                    <View style={styles.filename}>
                      <Text>{values.filename}</Text>
                    </View>
                    <View style={styles.elements}>
                      <Icon name='eye' type='font-awesome' color='green'
                        onPress={() => { this.showMedia(values) }}
                      />
                    </View>
                    <View style={styles.elements}>
                      <Icon name='trash' type='font-awesome' color='red'
                        onPress={() => { this.showDialog(values) }}
                      />
                    </View>
                  </View>
                )
              })
            }
          </View>
        </ScrollView>
      );
    }
  }

  const styles = StyleSheet.create({
    container: {
      flex: 1,
      alignItems: 'center',
      marginTop: 5,
      marginBottom: 5,
      height: '100%',
      width: '100%'
    },
    modalContainer: {
      flex: 1,
      justifyContent: 'center',
      alignItems: 'center',
      backgroundColor: 'white',
    },
    imageStyle: {
      width: '80%',
      height: '80%',
      borderWidth: 2,
      borderColor: 'green'
    },
    activityIndicator: {
      position: 'absolute',
      top: '50%',
      left: '45%',
    },
    viewButton: {
      backgroundColor: 'green',
      width: 73,
      height: 30
    },
    deleteButton: {
      backgroundColor: 'red',
      width: 80,
      height: 30
    },
    closeButton: {
      position: 'absolute',
      bottom: 10,
      left: '35%'
    },
    row: {
      flexDirection: 'row',
      alignItems: 'center',
      borderWidth: 2,
      height: 100,
      width: 350,
    },
    titleRow: {
      flexDirection: 'row',
      alignItems: 'center',
      borderWidth: 2,
      width: 350,
    },
    elements: {
      alignItems: 'center',
      width: 50,
      paddingTop: 5,
      paddingBottom: 5,
    },
    filename: {
      alignItems: 'center',
      width: 110,
      marginRight: 5,
      marginLeft: 5
    },
    title: {
      fontSize: 16,
      alignItems: 'center',
      color: 'blue',
      fontWeight: 'bold'
    }
  });
